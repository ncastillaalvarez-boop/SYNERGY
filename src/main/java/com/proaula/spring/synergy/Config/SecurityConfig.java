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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
<<<<<<< HEAD
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
=======
                 // Tiempo máximo de inactividad: 30 minutos
                .invalidSessionUrl("/login?sessionExpirada=true")
                 // Solo permitir una sesión activa por usuario
                 .maximumSessions(1)
                 .expiredUrl("/login?sessionExpirada=true")
>>>>>>> 6ddf0c7c6514921c117eaabd35ece8f7a1420779
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
                    "/css/**", "/js/**", "/img/**", "/IMG/**", "/images/**"
                ).permitAll()

                // ── Solo JWT para API ──
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/lider/**").hasAnyRole("ADMINISTRADOR", "LIDER")
                .requestMatchers("/api/**").authenticated()

                // ── Rutas Thymeleaf controladas por sesión ──
                .anyRequest().permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("SYNERGY_SESSION")
                .permitAll()
            );
      
        return http.build();
    }
   
}
