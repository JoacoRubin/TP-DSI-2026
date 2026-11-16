package ar.utn.donatrack.donaciones.configuracion;

import ar.utn.donatrack.donaciones.asignacion.application.ActualizarPesos;
import ar.utn.donatrack.donaciones.asignacion.application.ConfirmarAsignacion;
import ar.utn.donatrack.donaciones.asignacion.application.ConsultasAsignacion;
import ar.utn.donatrack.donaciones.asignacion.application.EjecutarMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.application.EvaluadorDonaciones;
import ar.utn.donatrack.donaciones.asignacion.application.EvaluarDonacion;
import ar.utn.donatrack.donaciones.asignacion.domain.CompatibilidadSemantica;
import ar.utn.donatrack.donaciones.asignacion.domain.MotorMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.PrioridadSubAtendidos;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.application.ActualizarEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.ActualizarNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.ConsultasBeneficiarias;
import ar.utn.donatrack.donaciones.beneficiarias.application.DarDeBajaEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.DarDeBajaNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.RegistrarEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.RegistrarNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.compartido.application.ServicioCuentas;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.donaciones.compartido.application.Transaccion;
import ar.utn.donatrack.donaciones.compartido.infrastructure.integraciones.ServicioCuentasRest;
import ar.utn.donatrack.donaciones.compartido.infrastructure.integraciones.ServicioLogisticaRest;
import ar.utn.donatrack.donaciones.donaciones.application.ActualizarDonacion;
import ar.utn.donatrack.donaciones.donaciones.application.ConsultasDonaciones;
import ar.utn.donatrack.donaciones.donaciones.application.DevolverADeposito;
import ar.utn.donatrack.donaciones.donaciones.application.EliminarDonacion;
import ar.utn.donatrack.donaciones.donaciones.application.MarcarDonacionVencida;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarCargaDonacion;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaNoSatisfactoria;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaPlanificada;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaRealizada;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarInicioDeRuta;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donantes.application.ActualizarContactoDonante;
import ar.utn.donatrack.donaciones.donantes.application.ConsultasDonantes;
import ar.utn.donatrack.donaciones.donantes.application.DarDeBajaDonante;
import ar.utn.donatrack.donaciones.donantes.application.NotificarDonantesInactivos;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarInteraccion;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarPersonaHumana;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarPersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.importacion.application.ConsultasImportacion;
import ar.utn.donatrack.donaciones.importacion.application.IniciarImportacion;
import ar.utn.donatrack.donaciones.importacion.application.ProcesarImportacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;
import ar.utn.donatrack.soporte.seguridad.TokenServicioProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Composition root del Servicio de Donaciones. Los casos de uso son POJOs: Spring sólo los
 * instancia y les inyecta adaptadores. Cambiar un adaptador = cambiar UNA línea acá.
 */
@Configuration
public class ConfiguracionCasosDeUso {

    // ------------------------------------------------------------- adaptadores de salida

    private static RestClient cliente(String url, TokenServicioProvider tokens) {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build());
        f.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl(url).requestFactory(f).requestInterceptor(tokens.interceptor()).build();
    }

    @Bean
    ServicioCuentas servicioCuentas(PropiedadesDonaciones p, TokenServicioProvider tokens) {
        return new ServicioCuentasRest(cliente(p.urlAutenticacion(), tokens));
    }

    @Bean
    ServicioLogistica servicioLogistica(PropiedadesDonaciones p, TokenServicioProvider tokens) {
        return new ServicioLogisticaRest(cliente(p.urlLogistica(), tokens));
    }

    @Bean
    Transaccion transaccion(PlatformTransactionManager tm) {
        TransactionTemplate template = new TransactionTemplate(tm);
        return new Transaccion() {
            @Override
            public <T> T ejecutar(Supplier<T> trabajo) {
                return template.execute(status -> trabajo.get());
            }
        };
    }

    @Bean
    Enlaces enlaces(PropiedadesDonaciones p) {
        return new Enlaces(p.urlFront());
    }

    @Bean
    AltaDeCuenta altaDeCuenta(ServicioCuentas c, Notificador n, Enlaces e) {
        return new AltaDeCuenta(c, n, e);
    }

    // ------------------------------------------------------------- donantes

    @Bean
    RegistrarPersonaHumana registrarPersonaHumana(DonanteRepositorio r, AltaDeCuenta a, Clock reloj) {
        return new RegistrarPersonaHumana(r, a, reloj);
    }

    @Bean
    RegistrarPersonaJuridica registrarPersonaJuridica(DonanteRepositorio r, AltaDeCuenta a, Clock reloj) {
        return new RegistrarPersonaJuridica(r, a, reloj);
    }

    @Bean
    ActualizarContactoDonante actualizarContactoDonante(DonanteRepositorio r) {
        return new ActualizarContactoDonante(r);
    }

    @Bean
    DarDeBajaDonante darDeBajaDonante(DonanteRepositorio r) {
        return new DarDeBajaDonante(r);
    }

    @Bean
    RegistrarInteraccion registrarInteraccion(DonanteRepositorio r, Clock reloj) {
        return new RegistrarInteraccion(r, reloj);
    }

    @Bean
    NotificarDonantesInactivos notificarDonantesInactivos(DonanteRepositorio r, Notificador n, Enlaces e, Clock reloj) {
        return new NotificarDonantesInactivos(r, n, e, reloj);
    }

    @Bean
    ConsultasDonantes consultasDonantes(DonanteRepositorio r) {
        return new ConsultasDonantes(r);
    }

    // ------------------------------------------------------------- beneficiarias

    @Bean
    RegistrarEntidad registrarEntidad(EntidadRepositorio r, AltaDeCuenta a, Clock reloj) {
        return new RegistrarEntidad(r, a, reloj);
    }

    @Bean
    ActualizarEntidad actualizarEntidad(EntidadRepositorio r) {
        return new ActualizarEntidad(r);
    }

    @Bean
    DarDeBajaEntidad darDeBajaEntidad(EntidadRepositorio e, NecesidadRepositorio n) {
        return new DarDeBajaEntidad(e, n);
    }

    @Bean
    RegistrarNecesidad registrarNecesidad(EntidadRepositorio e, NecesidadRepositorio n, CatalogoRepositorio c, Clock reloj) {
        return new RegistrarNecesidad(e, n, c, reloj);
    }

    @Bean
    ActualizarNecesidad actualizarNecesidad(NecesidadRepositorio n) {
        return new ActualizarNecesidad(n);
    }

    @Bean
    DarDeBajaNecesidad darDeBajaNecesidad(NecesidadRepositorio n) {
        return new DarDeBajaNecesidad(n);
    }

    @Bean
    ConsultasBeneficiarias consultasBeneficiarias(EntidadRepositorio e, NecesidadRepositorio n) {
        return new ConsultasBeneficiarias(e, n);
    }

    // ------------------------------------------------------------- donaciones

    @Bean
    RegistrarCargaDonacion registrarCargaDonacion(DonanteRepositorio dn, DonacionRepositorio d, CatalogoRepositorio c,
                                                  PublicadorEventos ev, Clock reloj) {
        return new RegistrarCargaDonacion(dn, d, c, ev, reloj);
    }

    @Bean
    ActualizarDonacion actualizarDonacion(DonacionRepositorio d) {
        return new ActualizarDonacion(d);
    }

    @Bean
    EliminarDonacion eliminarDonacion(DonacionRepositorio d, ResultadoMatchmakingRepositorio r) {
        return new EliminarDonacion(d, r);
    }

    @Bean
    MarcarDonacionVencida marcarDonacionVencida(DonacionRepositorio d, NecesidadRepositorio n,
                                                ResultadoMatchmakingRepositorio r, ServicioLogistica l, Clock reloj) {
        return new MarcarDonacionVencida(d, n, r, l, reloj);
    }

    @Bean
    DevolverADeposito devolverADeposito(DonacionRepositorio d, NecesidadRepositorio n, ServicioLogistica l, Clock reloj) {
        return new DevolverADeposito(d, n, l, reloj);
    }

    @Bean
    ConsultasDonaciones consultasDonaciones(DonacionRepositorio d) {
        return new ConsultasDonaciones(d);
    }

    @Bean
    RegistrarEntregaPlanificada registrarEntregaPlanificada(DonacionRepositorio d, Clock reloj) {
        return new RegistrarEntregaPlanificada(d, reloj);
    }

    @Bean
    RegistrarInicioDeRuta registrarInicioDeRuta(DonacionRepositorio d, DonanteRepositorio dn, EntidadRepositorio e,
                                                Notificador n, Enlaces en) {
        return new RegistrarInicioDeRuta(d, dn, e, n, en);
    }

    @Bean
    RegistrarEntregaRealizada registrarEntregaRealizada(DonacionRepositorio d, NecesidadRepositorio ne,
                                                        DonanteRepositorio dn, EntidadRepositorio e, Notificador n,
                                                        PublicadorEventos ev, Clock reloj) {
        return new RegistrarEntregaRealizada(d, ne, dn, e, n, ev, reloj);
    }

    @Bean
    RegistrarEntregaNoSatisfactoria registrarEntregaNoSatisfactoria(DonacionRepositorio d, DonanteRepositorio dn,
                                                                    EntidadRepositorio e, Notificador n,
                                                                    PropiedadesDonaciones p) {
        return new RegistrarEntregaNoSatisfactoria(d, dn, e, n, p.emailsAdministracion() == null ? List.of() : p.emailsAdministracion());
    }

    // ------------------------------------------------------------- asignación

    @Bean
    MotorMatchmaking motorMatchmaking(ResultadoMatchmakingRepositorio r, PropiedadesDonaciones p) {
        return new MotorMatchmaking(List.of(
                new CompatibilidadSemantica(r::pesos),
                new PrioridadSubAtendidos(p.suavizadoSubAtendidos())));
    }

    @Bean
    EvaluadorDonaciones evaluadorDonaciones(MotorMatchmaking m, NecesidadRepositorio n, DonacionRepositorio d,
                                            ResultadoMatchmakingRepositorio r, Clock reloj) {
        return new EvaluadorDonaciones(m, n, d, r, reloj);
    }

    @Bean
    EjecutarMatchmaking ejecutarMatchmaking(EvaluadorDonaciones ev, DonacionRepositorio d) {
        return new EjecutarMatchmaking(ev, d);
    }

    @Bean
    EvaluarDonacion evaluarDonacion(EvaluadorDonaciones ev, DonacionRepositorio d) {
        return new EvaluarDonacion(ev, d);
    }

    @Bean
    ConfirmarAsignacion confirmarAsignacion(DonacionRepositorio d, NecesidadRepositorio n, EntidadRepositorio e,
                                            DonanteRepositorio dn, ResultadoMatchmakingRepositorio r,
                                            ServicioLogistica l, Notificador no, Enlaces en, Clock reloj) {
        return new ConfirmarAsignacion(d, n, e, dn, r, l, no, en, reloj);
    }

    @Bean
    ActualizarPesos actualizarPesos(ResultadoMatchmakingRepositorio r) {
        return new ActualizarPesos(r);
    }

    @Bean
    ConsultasAsignacion consultasAsignacion(DonacionRepositorio d, ResultadoMatchmakingRepositorio r,
                                            NecesidadRepositorio n, EntidadRepositorio e) {
        return new ConsultasAsignacion(d, r, n, e);
    }

    // ------------------------------------------------------------- importación

    @Bean
    IniciarImportacion iniciarImportacion(ImportacionRepositorio i, Clock reloj) {
        return new IniciarImportacion(i, reloj);
    }

    @Bean
    ProcesarImportacion procesarImportacion(ImportacionRepositorio i, DonanteRepositorio d, ServicioCuentas c,
                                            AltaDeCuenta a, Transaccion t, Clock reloj) {
        return new ProcesarImportacion(i, d, c, a, t, reloj);
    }

    @Bean
    ConsultasImportacion consultasImportacion(ImportacionRepositorio i) {
        return new ConsultasImportacion(i);
    }
}
