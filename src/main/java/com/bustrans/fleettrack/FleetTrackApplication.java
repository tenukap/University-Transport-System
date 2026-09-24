package com.bustrans.fleettrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class FleetTrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(FleetTrackApplication.class, args);
	}

}
