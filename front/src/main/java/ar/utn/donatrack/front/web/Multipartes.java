package ar.utn.donatrack.front.web;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Reenvía archivos subidos por el navegador hacia un servicio (multipart → multipart). */
public final class Multipartes {

    private Multipartes() {
    }

    public static MultiValueMap<String, Object> de(String parte, List<MultipartFile> archivos) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        if (archivos != null) {
            for (MultipartFile a : archivos) {
                if (a != null && !a.isEmpty()) {
                    partes.add(parte, entidad(a));
                }
            }
        }
        return partes;
    }

    public static HttpEntity<ByteArrayResource> entidad(MultipartFile a) {
        try {
            byte[] bytes = a.getBytes();
            ByteArrayResource recurso = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return a.getOriginalFilename() == null ? "archivo" : a.getOriginalFilename();
                }
            };
            HttpHeaders h = new HttpHeaders();
            h.setContentType(a.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(a.getContentType()));
            return new HttpEntity<>(recurso, h);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
