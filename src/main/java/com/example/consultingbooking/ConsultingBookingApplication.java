package com.example.consultingbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConsultingBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultingBookingApplication.class, args);
    }
}
