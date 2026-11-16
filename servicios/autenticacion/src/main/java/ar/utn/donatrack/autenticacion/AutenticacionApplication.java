package ar.utn.donatrack.autenticacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AutenticacionApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutenticacionApplication.class, args);
    }
}
