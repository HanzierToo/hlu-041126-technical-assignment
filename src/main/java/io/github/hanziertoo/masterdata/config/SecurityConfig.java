package io.github.hanziertoo.masterdata.config;

import io.github.hanziertoo.masterdata.model.AppUser;
import io.github.hanziertoo.masterdata.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            AppUser user = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

            return User.withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .disabled(!user.isEnabled())
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    ApplicationRunner bootstrapUser(UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap.username}") String username,
            @Value("${app.bootstrap.password}") String password) {
        return args -> {
            if (username.isBlank() || password.isBlank()) {
                if (users.count() == 0) {
                    throw new IllegalStateException(
                            "Set app.bootstrap.username and app.bootstrap.password in application.yml "
                                    + "to create the initial user");
                }
                return;
            }

            validateBootstrapCredentials(username, password);

            if (users.findByUsername(username).isEmpty()) {
                users.save(new AppUser(username, encoder.encode(password)));
            }
        };
    }

    private void validateBootstrapCredentials(String username, String password) {
        if (username.length() > 100 || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Bootstrap username must be at most 100 characters and password "
                            + "at least 12 characters and at most 72 UTF-8 bytes");
        }
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorization -> authorization
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/error").permitAll()
                .anyRequest().authenticated());

        http.csrf(Customizer.withDefaults());

        http.formLogin(login -> login
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> writeLoginSuccess(response))
                .failureHandler((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials")));

        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied or invalid CSRF token")));

        return http.build();
    }

    private void writeLoginSuccess(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"authenticated\"}");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
