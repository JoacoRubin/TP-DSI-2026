package ar.utn.donatrack.logistica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class LogisticaApplication {
    public static void main(String[] args) throws IOException {
        // El MongoDB embebido (perfil local) requiere que exista su directorio de datos.
        Files.createDirectories(Path.of("data", "logistica", "mongo"));
        SpringApplication.run(LogisticaApplication.class, args);
    }
}
