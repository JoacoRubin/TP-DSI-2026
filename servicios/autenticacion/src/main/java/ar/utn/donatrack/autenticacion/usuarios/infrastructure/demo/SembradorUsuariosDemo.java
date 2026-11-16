package ar.utn.donatrack.autenticacion.usuarios.infrastructure.demo;

import ar.utn.donatrack.autenticacion.usuarios.application.NuevoUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

import static ar.utn.donatrack.contratos.demo.DatosDemo.*;

/** Cuentas de demostración (sólo perfil local). Idempotente: no duplica si ya existen. */
@Component
@Profile("local")
class SembradorUsuariosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorUsuariosDemo.class);
    private final RegistrarUsuario registrar;
    private final UsuarioRepositorio usuarios;

    SembradorUsuariosDemo(RegistrarUsuario registrar, UsuarioRepositorio usuarios) {
        this.registrar = registrar;
        this.usuarios = usuarios;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<NuevoUsuario> demo = List.of(
                new NuevoUsuario(ADMIN_EMAIL, "Administración DonaTrack", Rol.ADMIN, null, ADMIN.toString(), CLAVE_DEMO),
                new NuevoUsuario(CHOFER_EMAIL, "Carlos Chofer", Rol.CHOFER, null, CAMION_1_PATENTE, CLAVE_DEMO),
                new NuevoUsuario(DONANTE_ANA_EMAIL, "Ana Pérez", Rol.DONANTE, "HUMANA", DONANTE_ANA.toString(), CLAVE_DEMO),
                new NuevoUsuario(DONANTE_ARCOS_EMAIL, "Arcos Plateados S.A.", Rol.DONANTE, "JURIDICA", DONANTE_ARCOS.toString(), CLAVE_DEMO),
                new NuevoUsuario(ENTIDAD_ESCUELA_EMAIL, "Escuela Rural N°10", Rol.BENEFICIARIA, null, ENTIDAD_ESCUELA.toString(), CLAVE_DEMO),
                new NuevoUsuario(ENTIDAD_COMEDOR_EMAIL, "Comedor Escobar Sonrisas", Rol.BENEFICIARIA, null, ENTIDAD_COMEDOR.toString(), CLAVE_DEMO),
                new NuevoUsuario(ENTIDAD_HOGAR_EMAIL, "Hogar de Niños Medrano", Rol.BENEFICIARIA, null, ENTIDAD_HOGAR.toString(), CLAVE_DEMO));
        int creados = 0;
        for (NuevoUsuario u : demo) {
            if (!usuarios.existeEmail(u.email())) {
                registrar.ejecutar(u);
                creados++;
            }
        }
        log.info("Usuarios demo: {} creados (clave para todos: {})", creados, CLAVE_DEMO);
    }
}
