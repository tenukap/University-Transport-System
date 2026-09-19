package com.transport.uni_transport_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class UniTransportSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(UniTransportSystemApplication.class, args);
	}

}
