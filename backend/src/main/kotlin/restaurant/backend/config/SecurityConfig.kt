package restaurant.backend.config

import lombok.RequiredArgsConstructor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import restaurant.backend.filter.JwtFilter

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val jwtFilter: JwtFilter) {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .httpBasic { configurer: HttpBasicConfigurer<HttpSecurity> -> configurer.disable() }
            .csrf { csrf: CsrfConfigurer<HttpSecurity> -> csrf.disable() }
            .sessionManagement { management: SessionManagementConfigurer<HttpSecurity> ->
                management.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/auth/login", "/auth/token", "/auth/refresh").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/user/**").hasRole("USER")
                    .anyRequest().authenticated()
            }
            .addFilterAfter(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
