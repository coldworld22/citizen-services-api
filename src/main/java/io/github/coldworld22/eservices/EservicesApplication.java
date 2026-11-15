package io.github.coldworld22.eservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EservicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(EservicesApplication.class, args);
    }
}
