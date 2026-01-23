package br.com.fourzerofourdev.salesanalyticsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SalesAnalyticsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesAnalyticsBackendApplication.class, args);
    }
}