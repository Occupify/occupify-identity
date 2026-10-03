package com.occupify.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {ReactiveUserDetailsServiceAutoConfiguration.class})
public class OccupifyIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(OccupifyIdentityApplication.class, args);
    }
}
