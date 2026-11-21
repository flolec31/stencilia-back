package com.stencilia.stenciliaback.security

import com.stencilia.stenciliaback.auth.Role
import com.stencilia.stenciliaback.metrics.config.MetricsProperties
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtConfig::class, CorsConfig::class, MetricsProperties::class)
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val corsConfig: CorsConfig,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = corsConfig.allowedOrigins
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    // Registered before the JWT chain (first-match-wins) so /actuator/prometheus is guarded by a
    // basic-auth scrape credential, not the JWT resource server. Blank credential → no user → locked.
    @Bean
    @Order(1)
    fun prometheusSecurityFilterChain(
        http: HttpSecurity,
        passwordEncoder: PasswordEncoder,
        metricsProperties: MetricsProperties,
    ): SecurityFilterChain {
        val users = InMemoryUserDetailsManager()
        if (metricsProperties.username.isNotBlank() && metricsProperties.password.isNotBlank()) {
            users.createUser(
                User.withUsername(metricsProperties.username)
                    .password(passwordEncoder.encode(metricsProperties.password))
                    .roles("METRICS")
                    .build()
            )
        }
        return http
            .securityMatcher("/actuator/prometheus")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .httpBasic { }
            .userDetailsService(users)
            .build()
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtDecoder: JwtDecoder,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/auth/artist/change-password").hasRole("ARTIST")
                it.requestMatchers("/auth/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/artists/slug-available").permitAll()
                it.requestMatchers(HttpMethod.GET, "/artists/me").hasRole("ARTIST")
                it.requestMatchers(HttpMethod.GET, "/artists/{slug}").permitAll()
                it.requestMatchers("/swagger-ui/**").permitAll()
                it.requestMatchers("/v3/api-docs/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/appointment/{slug}").permitAll()
                it.requestMatchers(HttpMethod.POST, "/appointment/{slug}/reference").permitAll()
                it.requestMatchers(HttpMethod.POST, "/appointment/{slug}/links").permitAll()
                it.requestMatchers(HttpMethod.GET, "/appointment/verify").permitAll()
                it.requestMatchers(HttpMethod.GET, "/client/appointment/{appointmentId}").hasRole("CLIENT")
                it.requestMatchers(HttpMethod.GET, "/client/appointment/{appointmentId}/messages").hasRole("CLIENT")
                it.requestMatchers(HttpMethod.POST, "/client/appointment/{appointmentId}/messages").hasRole("CLIENT")
                it.requestMatchers(HttpMethod.POST, "/client/appointment/{appointmentId}/messages/image").hasRole("CLIENT")
                it.anyRequest().hasRole("ARTIST")
            }
            .oauth2ResourceServer {
                it.jwt { jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter) }
                it.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .build()

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val role = Role.fromClaim(jwt.getClaimAsString("type"))
            if (role != null) listOf(SimpleGrantedAuthority(role.authority)) else emptyList()
        }
        return converter
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun hmacSecretKey(jwtConfig: JwtConfig): SecretKeySpec =
        SecretKeySpec(Base64.getDecoder().decode(jwtConfig.secret), "HmacSHA256")

    @Bean
    fun jwtDecoder(secretKey: SecretKeySpec): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).build()

    @Bean
    fun jwtEncoder(secretKey: SecretKeySpec): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(secretKey).algorithm(JWSAlgorithm.HS256).build()
        return NimbusJwtEncoder(ImmutableJWKSet(JWKSet(jwk)))
    }
}
