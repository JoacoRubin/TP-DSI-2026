package ar.utn.donatrack.logistica.compartido.infrastructure;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fotos de las entregas recibidas. En producción: object storage (S3/GCS) + CDN. */
@Component
public class AlmacenFotos {

    private static final Map<String, String> TIPOS = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAXIMO_BYTES = 5 * 1024 * 1024;
    private final Path directorio;

    AlmacenFotos(@Value("${donatrack.fotos.directorio:./data/logistica/fotos}") String directorio) throws IOException {
        this.directorio = Files.createDirectories(Path.of(directorio)).toAbsolutePath().normalize();
    }

    public List<String> guardar(List<MultipartFile> archivos) {
        List<String> urls = new ArrayList<>();
        if (archivos == null) {
            return urls;
        }
        for (MultipartFile a : archivos) {
            if (a == null || a.isEmpty()) {
                continue;
            }
            String extension = TIPOS.get(a.getContentType());
            if (extension == null) {
                throw new ReglaDeNegocioException("Formato de imagen no soportado (usá JPG, PNG o WEBP)");
            }
            if (a.getSize() > MAXIMO_BYTES) {
                throw new ReglaDeNegocioException("Cada imagen debe pesar menos de 5 MB");
            }
            String nombre = UUID.randomUUID() + extension;
            try (InputStream in = a.getInputStream()) {
                Files.copy(in, directorio.resolve(nombre));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo guardar la imagen", e);
            }
            urls.add("/api/fotos/" + nombre);
        }
        return urls;
    }

    /** Usado por los datos demo: genera una ilustración SVG simple. */
    public String guardarSvg(String nombre, String texto, String color) {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
                  <rect width="640" height="420" fill="%s"/>
                  <circle cx="320" cy="170" r="80" fill="#ffffff" opacity="0.25"/>
                  <text x="320" y="185" font-family="sans-serif" font-size="64" text-anchor="middle" fill="#ffffff">&#10084;</text>
                  <text x="320" y="320" font-family="sans-serif" font-size="30" text-anchor="middle" fill="#ffffff">%s</text>
                </svg>""".formatted(color, texto);
        try {
            Files.writeString(directorio.resolve(nombre), svg, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return "/api/fotos/" + nombre;
    }

    public Path resolver(String nombre) {
        Path p = directorio.resolve(nombre).normalize();
        if (!p.startsWith(directorio) || !Files.isRegularFile(p)) {
            throw new ReglaDeNegocioException("Imagen inexistente");
        }
        return p;
    }
}
