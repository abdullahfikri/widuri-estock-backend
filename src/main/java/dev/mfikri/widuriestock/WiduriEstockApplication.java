package dev.mfikri.widuriestock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WiduriEstockApplication {
    public static void main(String[] args) {
        SpringApplication.run(WiduriEstockApplication.class, args);
    }


}
