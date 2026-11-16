package ar.utn.donatrack.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IncentivosApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncentivosApplication.class, args);
    }
}
