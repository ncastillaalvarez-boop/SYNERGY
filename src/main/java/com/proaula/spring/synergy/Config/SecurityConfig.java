package com.proaula.spring.synergy.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.proaula.spring.synergy.Security.GoogleLoginSuccessHandler;
import com.proaula.spring.synergy.Security.JwtAuthenticationFilter;
import com.proaula.spring.synergy.Security.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           GoogleLoginSuccessHandler googleLoginSuccessHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // Permite que la app Flutter en el navegador (flutter run -d edge/chrome) llame a /api/**
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?sessionExpirada=true")
            )
            .authorizeHttpRequests(authz -> authz

                // ── Rutas públicas (INVITADO sin login) ──
                .requestMatchers(
                    "/", "/index", "/login", "/logout",
                    "/seleccionar-rol",
                    "/registro/**",
                    "/registrarse",
                    "/publico/**",
                    "/proyectos/publico",
                    "/error",
                    "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico",
                    "/login/google", "/oauth2/**", "/login/oauth2/**"
                ).permitAll()

                // La autenticación inicial debe ser pública; el resto de la API
                // conserva la protección por JWT definida a continuación.
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()

                // ── Solo JWT para API ──
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/lider/**").hasAnyRole("ADMINISTRADOR", "LIDER")
                .requestMatchers("/api/**").authenticated()

                // ── Rutas Thymeleaf controladas por sesión ──
                .anyRequest().permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            // La API REST (app móvil) responde 401 en JSON en vez de redirigir al login web
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
            )
            // ── Inicio de sesión con Google ──
            // El botón "Continuar con Google" apunta a /oauth2/authorization/google.
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler(googleLoginSuccessHandler)
                .failureUrl("/login?error=google")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("SYNERGY_SESSION")
                .permitAll()
            );
      
        return http.build();
    }

    /**
     * CORS solo para la API REST. Flutter web corre en otro puerto (http://localhost:XXXXX),
     * y sin esto el navegador bloquea las peticiones y la app dice "No se pudo conectar".
     * Se usa token JWT (no cookies), por eso no hace falta allowCredentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*",
                "http://10.*:*", "http://192.168.*:*"));
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        api.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        api.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", api);
        return source;
    }
   
}
