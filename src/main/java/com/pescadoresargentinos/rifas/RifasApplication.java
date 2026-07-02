package com.pescadoresargentinos.rifas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RifasApplication {

    public static void main(String[] args) {
        SpringApplication.run(RifasApplication.class, args);
    }
}
