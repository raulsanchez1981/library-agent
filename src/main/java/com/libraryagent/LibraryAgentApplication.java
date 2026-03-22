package com.libraryagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibraryAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryAgentApplication.class, args);
    }
}
