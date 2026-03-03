package com.ben.booking;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class BookingServiceApplication {

	@Value("${spring.data.mongodb.uri}")
	private String mongoUri;

	@PostConstruct
	public void logUri() {
		log.info("MongoDB URI: {}", mongoUri);
	}

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

}
