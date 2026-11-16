package ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad;

import ar.utn.donatrack.autenticacion.usuarios.application.HasheadorClaves;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptHasheadorClaves implements HasheadorClaves {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hashear(String clave) {
        return encoder.encode(clave);
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return hash != null && encoder.matches(clave, hash);
    }
}
