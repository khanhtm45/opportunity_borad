package com.opportunityboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpportunityBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpportunityBoardApplication.class, args);
    }
}
