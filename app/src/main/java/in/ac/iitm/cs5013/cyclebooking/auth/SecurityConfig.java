package in.ac.iitm.cs5013.cyclebooking.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login is by institute smail address plus the resident's passcode, per ADR 0004.
 * Institute OAuth2 needs a Google Cloud client registered against the smail domain,
 * which requires IT approval we do not control; this is the plan-B path from the
 * proposal's risk 2, and the smail address stays the identity key either way.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/css/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/cycles", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
