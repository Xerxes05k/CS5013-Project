package in.ac.iitm.cs5013.cyclebooking;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Time comes from an injected Clock rather than Instant.now() so the booking rules
 * can be tested at exact boundaries with a fixed clock.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
