package ar.utn.donatrack.front;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Cliente liviano (SSR): renderiza vistas con Thymeleaf consumiendo las APIs REST. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FrontApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrontApplication.class, args);
    }
}
