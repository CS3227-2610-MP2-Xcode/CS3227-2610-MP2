package io.github.cs32272610mp2xcode.finderskeepers.appointment.bootstrap;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.OfficerAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.ui.OfficerAppointmentPane;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.ui.StudentAppointmentPane;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap.ClaimWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;

/** Composes fresh authenticated Student and Desk Officer appointment features. */
public final class AppointmentWorkspaceFactory {
    private final AppointmentRepository repository;

    private final ClaimWorkspaceFactory claims;

    private final Clock clock;

    private final Supplier<UUID> ids;

    /** Creates a production appointment feature factory.
     * @param storePath appointment store path
     *  @param claimFactory Claim workspace boundary */
    public AppointmentWorkspaceFactory(Path storePath, ClaimWorkspaceFactory claimFactory) {
        this(new JsonAppointmentRepository(Objects.requireNonNull(storePath, "storePath")),
                claimFactory, Clock.systemUTC(), UUID::randomUUID);
    }

    /** Creates an appointment feature factory over injected dependencies.
     * @param appointments appointment repository
     *  @param claimFactory Claim workspace boundary
     *  @param eventClock clock for domain events
     *  @param idSupplier UUID source */
    public AppointmentWorkspaceFactory(AppointmentRepository appointments,
            ClaimWorkspaceFactory claimFactory, Clock eventClock, Supplier<UUID> idSupplier) {
        repository = Objects.requireNonNull(appointments, "appointments");
        claims = Objects.requireNonNull(claimFactory, "claimFactory");
        clock = Objects.requireNonNull(eventClock, "clock");
        ids = Objects.requireNonNull(idSupplier, "idSupplier");
    }

    /** Creates a Student appointment workspace feature.
     * @param user authenticated Student
     *  @return Student appointment feature */
    public WorkspaceFeature createStudentFeature(AuthenticatedUser user) {
        StudentAppointmentPane pane = new StudentAppointmentPane(new StudentAppointmentService(
                user, repository, claims.createApprovedClaimService(user), clock, ids));
        return new WorkspaceFeature(pane, pane::enter, pane::hasUnsavedText,
                pane::clearSessionState);
    }

    /** Creates a Desk Officer appointment workspace feature.
     * @param user authenticated Desk Officer
     *  @return Desk Officer appointment feature */
    public WorkspaceFeature createOfficerFeature(AuthenticatedUser user) {
        OfficerAppointmentPane pane = new OfficerAppointmentPane(
                new OfficerAppointmentService(user, repository, clock, ids));
        return new WorkspaceFeature(pane, pane::enter, pane::hasUnsavedText,
                pane::clearSessionState);
    }
}
