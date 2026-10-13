package com.anurag.cse.config;
import com.anurag.cse.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable()).cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // not logged in / expired token -> 401 so the frontend can send the user back to login
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/uploads/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/crops").hasRole("FARMER")
                .requestMatchers(HttpMethod.PUT, "/api/crops/*").hasRole("FARMER")
                .requestMatchers(HttpMethod.DELETE, "/api/crops/*").hasRole("FARMER")
                .requestMatchers("/api/crops/mine", "/api/orders/farmer", "/api/dashboard/**").hasRole("FARMER")
                .requestMatchers("/api/orders/*/accept", "/api/orders/*/reject").hasRole("FARMER")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("SHOPKEEPER")
                .requestMatchers("/api/orders/shop").hasRole("SHOPKEEPER")
                .requestMatchers("/api/orders/transport/**", "/api/orders/*/claim", "/api/orders/*/status").hasRole("TRANSPORTER")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}