package com.stencilia.stenciliaback.flash

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.TestcontainersConfiguration
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.artist.repository.TattooArtistRepository
import com.stencilia.stenciliaback.auth.Role
import com.stencilia.stenciliaback.auth.service.JwtService
import com.stencilia.stenciliaback.flash.entity.Flash
import com.stencilia.stenciliaback.flash.repository.FlashRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.time.Instant
import java.util.UUID

class FlashIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var artistRepository: TattooArtistRepository
    @Autowired lateinit var flashRepository: FlashRepository

    @Value($$"${app.storage.bucket}")
    lateinit var bucket: String

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(URI.create(TestcontainersConfiguration.s3Endpoint))
            .region(Region.EU_WEST_3)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(TestcontainersConfiguration.s3AccessKey, TestcontainersConfiguration.s3SecretKey)
                )
            )
            .forcePathStyle(true)
            .build()
    }

    private fun s3ObjectExists(key: String): Boolean = try {
        s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
        true
    } catch (_: NoSuchKeyException) {
        false
    }

    @BeforeEach
    @AfterEach
    fun cleanup() {
        flashRepository.deleteAll()
        artistRepository.deleteAll()
    }

    private fun createActivatedArtist(slug: String = "test-artist"): TattooArtist =
        artistRepository.save(
            TattooArtist(
                id = UUID.randomUUID(),
                email = "$slug@test.com",
                password = "hashed",
                artistName = "Test Artist",
                slug = slug,
                city = "Test City",
                countryCode = "FR",
                registeredAt = Instant.now(),
                activatedAt = Instant.now(),
            )
        )

    private fun tokenFor(artist: TattooArtist) = jwtService.generateAccessToken(artist.id, Role.ARTIST)

    private fun saveFlash(artist: TattooArtist, position: Int, caption: String? = null): Flash =
        flashRepository.save(
            Flash(
                artist = artist,
                imageKey = "flashes/${artist.id}/${UUID.randomUUID()}",
                caption = caption,
                position = position,
            )
        )

    private fun pngImage() = MockMultipartFile("image", "flash.png", "image/png", ByteArray(100))

    private fun createFlash(token: String?, image: MockMultipartFile? = pngImage(), caption: String? = null) =
        mockMvc.perform(
            multipart("/artists/me/flashes")
                .apply { image?.let { file(it) } }
                .apply { caption?.let { param("caption", it) } }
                .apply { token?.let { header("Authorization", "Bearer $it") } }
        )

    private fun orderedIds(artist: TattooArtist) =
        flashRepository.findByArtistIdOrderByPositionAsc(artist.id).map { it.id }

    // --- POST /artists/me/flashes ---

    @Test
    fun `create flash returns 201, stores the image and puts the flash first with its caption`() {
        val artist = createActivatedArtist()
        val existing = saveFlash(artist, 0)

        createFlash(tokenFor(artist), caption = "Rose and dagger")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isString)
            .andExpect(jsonPath("$.caption").value("Rose and dagger"))
            .andExpect(jsonPath("$.imageUrl").value(containsString("X-Amz-Signature")))

        val flashes = flashRepository.findByArtistIdOrderByPositionAsc(artist.id)
        assertThat(flashes.map { it.id }.last()).isEqualTo(existing.id)
        val created = flashes.first()
        assertThat(created.caption).isEqualTo("Rose and dagger")
        assertThat(created.imageKey).matches("flashes/${artist.id}/[0-9a-f\\-]{36}")
        assertThat(s3ObjectExists(created.imageKey)).isTrue()
    }

    @Test
    fun `create flash without caption saves no caption`() {
        val artist = createActivatedArtist()

        createFlash(tokenFor(artist))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.caption").value(null as String?))

        assertThat(flashRepository.findAll().single().caption).isNull()
    }

    @Test
    fun `create flash with blank caption saves no caption`() {
        val artist = createActivatedArtist()

        createFlash(tokenFor(artist), caption = "   ").andExpect(status().isCreated)

        assertThat(flashRepository.findAll().single().caption).isNull()
    }

    @Test
    fun `create flash with a 255 character caption is saved in full`() {
        val artist = createActivatedArtist()
        val atLimit = "a".repeat(255)

        createFlash(tokenFor(artist), caption = atLimit).andExpect(status().isCreated)

        assertThat(flashRepository.findAll().single().caption).isEqualTo(atLimit)
    }

    @Test
    fun `create flash with a caption over 255 characters returns 400 and saves nothing`() {
        val artist = createActivatedArtist()

        createFlash(tokenFor(artist), caption = "a".repeat(256)).andExpect(status().isBadRequest)

        assertThat(flashRepository.count()).isZero()
    }

    @Test
    fun `create flash returns 409 for a 51st flash`() {
        val artist = createActivatedArtist()
        repeat(50) { saveFlash(artist, it) }

        createFlash(tokenFor(artist))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("An artist can have at most 50 flashes"))

        assertThat(flashRepository.countByArtistId(artist.id)).isEqualTo(50)
    }

    @Test
    fun `create flash with invalid content type returns 400 and saves nothing`() {
        val artist = createActivatedArtist()

        createFlash(tokenFor(artist), MockMultipartFile("image", "flash.pdf", "application/pdf", ByteArray(100)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Image must be a JPEG, PNG, or WebP image"))

        assertThat(flashRepository.count()).isZero()
    }

    @Test
    fun `create flash without an image returns 400`() {
        val artist = createActivatedArtist()

        createFlash(tokenFor(artist), image = null, caption = "no image").andExpect(status().isBadRequest)

        assertThat(flashRepository.count()).isZero()
    }

    @Test
    fun `create flash returns 401 when unauthenticated`() {
        createFlash(token = null).andExpect(status().isUnauthorized)
    }

    @Test
    fun `create flash returns 403 for a client token`() {
        createFlash(jwtService.generateAccessToken(UUID.randomUUID(), Role.CLIENT)).andExpect(status().isForbidden)
    }

    // --- GET /artists/me/flashes ---

    @Test
    fun `get flashes returns the artist flashes in display order`() {
        val artist = createActivatedArtist()
        val second = saveFlash(artist, 1, "second")
        val first = saveFlash(artist, 0, "first")

        mockMvc.perform(get("/artists/me/flashes").header("Authorization", "Bearer ${tokenFor(artist)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(first.id.toString()))
            .andExpect(jsonPath("$[0].caption").value("first"))
            .andExpect(jsonPath("$[0].imageUrl").value(containsString("X-Amz-Signature")))
            .andExpect(jsonPath("$[1].id").value(second.id.toString()))
    }

    @Test
    fun `get flashes returns an empty list when the artist has none`() {
        val artist = createActivatedArtist()

        mockMvc.perform(get("/artists/me/flashes").header("Authorization", "Bearer ${tokenFor(artist)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `get flashes does not return another artist flashes`() {
        val artist = createActivatedArtist()
        saveFlash(createActivatedArtist(slug = "other-artist"), 0)

        mockMvc.perform(get("/artists/me/flashes").header("Authorization", "Bearer ${tokenFor(artist)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `get flashes returns 401 when unauthenticated`() {
        mockMvc.perform(get("/artists/me/flashes")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `public profile does not expose flashes`() {
        val artist = createActivatedArtist()
        saveFlash(artist, 0, "hidden")

        mockMvc.perform(get("/artists/${artist.slug}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.flashes").doesNotExist())
    }

    // --- PATCH /artists/me/flashes/{flashId} ---

    @Test
    fun `update caption returns 200 and changes only the caption`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(artist, 0, "old")
        val other = saveFlash(artist, 1)

        mockMvc.perform(
            patch("/artists/me/flashes/${flash.id}")
                .header("Authorization", "Bearer ${tokenFor(artist)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"caption":"new"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(flash.id.toString()))
            .andExpect(jsonPath("$.caption").value("new"))

        val saved = flashRepository.findById(flash.id).get()
        assertThat(saved.caption).isEqualTo("new")
        assertThat(saved.imageKey).isEqualTo(flash.imageKey)
        assertThat(orderedIds(artist)).containsExactly(flash.id, other.id)
    }

    @Test
    fun `update caption with null clears it`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(artist, 0, "old")

        mockMvc.perform(
            patch("/artists/me/flashes/${flash.id}")
                .header("Authorization", "Bearer ${tokenFor(artist)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"caption":null}""")
        ).andExpect(status().isOk)

        assertThat(flashRepository.findById(flash.id).get().caption).isNull()
    }

    @Test
    fun `update caption ignores an image key in the body`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(artist, 0)

        mockMvc.perform(
            patch("/artists/me/flashes/${flash.id}")
                .header("Authorization", "Bearer ${tokenFor(artist)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"caption":"c","imageKey":"flashes/${artist.id}/other"}""")
        ).andExpect(status().isOk)

        assertThat(flashRepository.findById(flash.id).get().imageKey).isEqualTo(flash.imageKey)
    }

    @Test
    fun `update caption over 255 characters returns 400 and keeps the old caption`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(artist, 0, "old")

        mockMvc.perform(
            patch("/artists/me/flashes/${flash.id}")
                .header("Authorization", "Bearer ${tokenFor(artist)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"caption":"${"a".repeat(256)}"}""")
        ).andExpect(status().isBadRequest)

        assertThat(flashRepository.findById(flash.id).get().caption).isEqualTo("old")
    }

    @Test
    fun `update caption returns 404 for another artist flash`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(createActivatedArtist(slug = "other-artist"), 0, "theirs")

        mockMvc.perform(
            patch("/artists/me/flashes/${flash.id}")
                .header("Authorization", "Bearer ${tokenFor(artist)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"caption":"mine"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Flash not found"))

        assertThat(flashRepository.findById(flash.id).get().caption).isEqualTo("theirs")
    }

    // --- DELETE /artists/me/flashes/{flashId} ---

    @Test
    fun `delete flash returns 204 and removes the flash and its image`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(artist, 0)
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(flash.imageKey).contentType("image/png").build(),
            RequestBody.fromBytes(ByteArray(10))
        )

        mockMvc.perform(delete("/artists/me/flashes/${flash.id}").header("Authorization", "Bearer ${tokenFor(artist)}"))
            .andExpect(status().isNoContent)

        assertThat(flashRepository.existsById(flash.id)).isFalse()
        assertThat(s3ObjectExists(flash.imageKey)).isFalse()
    }

    @Test
    fun `delete flash returns 404 for another artist flash`() {
        val artist = createActivatedArtist()
        val flash = saveFlash(createActivatedArtist(slug = "other-artist"), 0)

        mockMvc.perform(delete("/artists/me/flashes/${flash.id}").header("Authorization", "Bearer ${tokenFor(artist)}"))
            .andExpect(status().isNotFound)

        assertThat(flashRepository.existsById(flash.id)).isTrue()
    }

    @Test
    fun `delete flash returns 401 when unauthenticated`() {
        mockMvc.perform(delete("/artists/me/flashes/${UUID.randomUUID()}")).andExpect(status().isUnauthorized)
    }

    // --- PUT /artists/me/flashes/order ---

    private fun reorder(token: String, ids: List<Any>) =
        mockMvc.perform(
            put("/artists/me/flashes/order")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("flashIds" to ids)))
        )

    @Test
    fun `reorder returns 204 and persists the new order`() {
        val artist = createActivatedArtist()
        val a = saveFlash(artist, 0)
        val b = saveFlash(artist, 1)
        val c = saveFlash(artist, 2)

        reorder(tokenFor(artist), listOf(c.id, a.id, b.id)).andExpect(status().isNoContent)

        assertThat(orderedIds(artist)).containsExactly(c.id, a.id, b.id)
    }

    @Test
    fun `a flash created after a reorder goes first`() {
        val artist = createActivatedArtist()
        val a = saveFlash(artist, 0)
        val b = saveFlash(artist, 1)
        reorder(tokenFor(artist), listOf(b.id, a.id)).andExpect(status().isNoContent)

        createFlash(tokenFor(artist)).andExpect(status().isCreated)

        assertThat(orderedIds(artist).drop(1)).containsExactly(b.id, a.id)
    }

    @Test
    fun `reorder returns 400 when a flash is missing`() {
        val artist = createActivatedArtist()
        val a = saveFlash(artist, 0)
        val b = saveFlash(artist, 1)
        saveFlash(artist, 2)

        reorder(tokenFor(artist), listOf(b.id, a.id))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("The order must list each of your flashes exactly once"))
    }

    @Test
    fun `reorder returns 400 when a flash is listed twice`() {
        val artist = createActivatedArtist()
        val a = saveFlash(artist, 0)
        saveFlash(artist, 1)

        reorder(tokenFor(artist), listOf(a.id, a.id)).andExpect(status().isBadRequest)
    }

    @Test
    fun `reorder returns 400 and changes nothing when it includes another artist flash`() {
        val artist = createActivatedArtist()
        val a = saveFlash(artist, 0)
        val b = saveFlash(artist, 1)
        val foreign = saveFlash(createActivatedArtist(slug = "other-artist"), 0)

        reorder(tokenFor(artist), listOf(foreign.id, b.id)).andExpect(status().isBadRequest)

        assertThat(orderedIds(artist)).containsExactly(a.id, b.id)
    }

    @Test
    fun `reorder returns 400 when the list exceeds 50 ids`() {
        val artist = createActivatedArtist()

        reorder(tokenFor(artist), List(51) { UUID.randomUUID() }).andExpect(status().isBadRequest)
    }

    @Test
    fun `reorder returns 401 when unauthenticated`() {
        mockMvc.perform(
            put("/artists/me/flashes/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"flashIds":[]}""")
        ).andExpect(status().isUnauthorized)
    }
}
