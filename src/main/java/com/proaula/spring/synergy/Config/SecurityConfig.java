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

    /**
     * Content-Security-Policy: el navegador solo carga scripts, estilos, fuentes e imágenes de
     * Synergy y de los CDN que usan las páginas (Font Awesome, Google Fonts, Leaflet para el mapa
     * y los mosaicos de OpenStreetMap). Un script inyectado desde otro dominio no se ejecuta.
     */
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://unpkg.com",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com https://unpkg.com",
            "font-src 'self' data: https://fonts.gstatic.com https://cdnjs.cloudflare.com",
            "img-src 'self' data: blob: https://*.googleusercontent.com https://tile.openstreetmap.org https://*.tile.openstreetmap.org https://unpkg.com",
            "connect-src 'self'",
            "frame-ancestors 'self'",
            "base-uri 'self'",
            "form-action 'self'",
            "object-src 'none'");

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
            // CSRF activo para los formularios web (Thymeleaf agrega el token en cada th:action).
            // La API REST usa JWT en la cabecera Authorization (no cookies), por eso se excluye.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                // El formulario de login se excluye: si la página quedó abierta más que la sesión
                // (13 min), el token habría caducado y el usuario vería un error al entrar.
                PathPatternRequestMatcher.withDefaults().matcher(org.springframework.http.HttpMethod.POST, "/login")))
            // Cabeceras de seguridad
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(ref -> ref.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()"))
            )
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
                    "/login/google", "/oauth2/**", "/login/oauth2/**",
                    // Documentación de la API (Swagger UI)
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
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
                // Formulario enviado con la sesión vencida (token CSRF ausente o viejo): al login con aviso
                .accessDeniedHandler((request, response, denegado) -> {
                    if (denegado instanceof org.springframework.security.web.csrf.CsrfException
                            && !request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                        response.sendRedirect(request.getContextPath() + "/login?sessionExpirada=true");
                    } else {
                        response.sendError(HttpStatus.FORBIDDEN.value());
                    }
                })
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
                // Los enlaces "Cerrar sesión" son GET; con CSRF activo Spring solo aceptaría POST
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(org.springframework.http.HttpMethod.GET, "/logout"))
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
