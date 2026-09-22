package com.bustrans.fleettrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class FleetTrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetTrackApplication.class, args);
    }
}
