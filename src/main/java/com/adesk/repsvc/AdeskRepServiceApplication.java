package com.adesk.repsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdeskRepServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdeskRepServiceApplication.class, args);
    }
}
