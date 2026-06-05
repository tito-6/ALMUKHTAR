package com.mycompany.transfersystem;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        com.mycompany.transfersystem.config.NotificationThresholdProperties.class,
        com.mycompany.transfersystem.config.properties.AlmukhtarNotificationOutboxProperties.class,
        com.mycompany.transfersystem.config.properties.AlmukhtarWhatsAppProperties.class
})
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
public class TransferSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferSystemApplication.class, args);
    }
}
