package com.mycompany.transfersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransferSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferSystemApplication.class, args);
    }

}