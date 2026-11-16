package ar.utn.donatrack.donaciones.compartido.infrastructure;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Almacenamiento local de fotos de bienes. En la nube se reemplaza por un object storage (S3/GCS). */
@Component
public class AlmacenFotos {

    private static final Map<String, String> TIPOS = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAXIMO_BYTES = 5 * 1024 * 1024;
    private final Path directorio;

    AlmacenFotos(@Value("${donatrack.fotos.directorio:./data/donaciones/fotos}") String directorio) throws IOException {
        this.directorio = Files.createDirectories(Path.of(directorio)).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        String extension = TIPOS.get(archivo.getContentType());
        if (extension == null) {
            throw new ReglaDeNegocioException("Formato de imagen no soportado (usá JPG, PNG o WEBP)");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new ReglaDeNegocioException("La imagen supera los 5 MB");
        }
        String nombre = UUID.randomUUID() + extension;
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, directorio.resolve(nombre));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return nombre;
    }

    /** Resuelve el archivo evitando path traversal ("../../etc/passwd"). */
    public Path resolver(String nombre) {
        Path p = directorio.resolve(nombre).normalize();
        if (!p.startsWith(directorio) || !Files.isRegularFile(p)) {
            throw new ReglaDeNegocioException("Imagen inexistente");
        }
        return p;
    }
}
