package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;

/** Privacy-safe application diagnostics for appointment operations. */
public final class AppointmentDiagnostics {
    private static final Logger LOGGER = Logger.getLogger(
            AppointmentDiagnostics.class.getName());

    private AppointmentDiagnostics() {
    }

    /** Logs a typed appointment failure without retained user values.
     * @param operation operation name
     *  @param reason typed failure reason */
    public static void failure(String operation, AppointmentStoreException.Reason reason) {
        String correlation = UUID.randomUUID().toString();
        LOGGER.log(Level.WARNING, "appointment operation={0} outcome=FAILURE reason={1} "
                + "correlation={2}", new Object[] {operation, reason, correlation});
    }
}
