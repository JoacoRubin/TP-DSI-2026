package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Alta/actualización masiva (importación CSV). Una sola consulta por lote para detectar
 * existentes y un único guardado: evita el problema N+1 con 20.000 filas.
 */
public class RegistrarUsuariosEnLote {

    private final UsuarioRepositorio usuarios;
    private final GeneradorSecretos secretos;
    private final Clock reloj;

    public RegistrarUsuariosEnLote(UsuarioRepositorio usuarios, GeneradorSecretos secretos, Clock reloj) {
        this.usuarios = usuarios;
        this.secretos = secretos;
        this.reloj = reloj;
    }

    public List<ResultadoAlta> ejecutar(List<NuevoUsuario> lote) {
        Instant ahora = reloj.instant();
        List<String> emails = lote.stream().map(n -> Usuario.normalizarEmail(n.email())).toList();
        Map<String, Usuario> existentes = usuarios.buscarPorEmails(emails).stream()
                .collect(Collectors.toMap(Usuario::email, Function.identity()));

        List<Usuario> aGuardar = new ArrayList<>();
        List<ResultadoAlta> resultados = new ArrayList<>();
        for (NuevoUsuario n : lote) {
            String email = Usuario.normalizarEmail(n.email());
            Usuario existente = existentes.get(email);
            if (existente != null) {
                existente.actualizarDatos(n.nombre(), n.perfil(), n.referencia());
                aGuardar.add(existente);
                resultados.add(new ResultadoAlta(existente.id(), email, false, null));
            } else {
                String token = secretos.nuevo();
                Usuario nuevo = Usuario.pendienteDeActivacion(UUID.randomUUID(), email, n.nombre(), n.rol(), n.perfil(),
                        n.referencia(), TokensActivacion.hash(token), ahora.plus(TokensActivacion.VIGENCIA), ahora);
                existentes.put(email, nuevo);
                aGuardar.add(nuevo);
                resultados.add(new ResultadoAlta(nuevo.id(), email, true, token));
            }
        }
        usuarios.guardarTodos(aGuardar);
        return resultados;
    }
}
