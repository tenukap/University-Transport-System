package com.bustrans.fleettrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class UniTransportApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniTransportApplication.class, args);
    }
}
