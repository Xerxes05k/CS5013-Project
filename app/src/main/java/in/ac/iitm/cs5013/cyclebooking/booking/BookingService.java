package in.ac.iitm.cs5013.cyclebooking.booking;

import java.util.List;

/** Booking Module: availability and reservation over the cycle pool. */
public interface BookingService {

    List<CycleStatus> getAvailability();

    /**
     * Books the cycle for the resident, or throws {@link BookingRejectedException} with
     * a reason the resident can read. Week 3 adds the ID-card cross-check here.
     */
    BookingConfirmation createBooking(long residentId, long cycleId);
}
