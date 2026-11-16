package ar.utn.donatrack.donaciones.beneficiarias.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Período de renovación de una necesidad recurrente (semanas ISO: lunes a domingo). */
public enum Periodicidad {
    SEMANAL {
        @Override
        public LocalDate inicioPeriodo(LocalDate fecha) {
            return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
    },
    MENSUAL {
        @Override
        public LocalDate inicioPeriodo(LocalDate fecha) {
            return fecha.withDayOfMonth(1);
        }
    };

    public abstract LocalDate inicioPeriodo(LocalDate fecha);

    public boolean mismoPeriodo(LocalDate a, LocalDate b) {
        return inicioPeriodo(a).equals(inicioPeriodo(b));
    }
}
