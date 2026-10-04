package in.ac.iitm.cs5013.cyclebooking.rules;

/** Whether a resident may book now, and if not, a reason fit to show them. */
public record EligibilityResult(boolean allowed, Reason reason) {

    public enum Reason {
        OK("You can book a cycle."),
        OVERDUE_RETURN("Your current cycle is past the 24-hour limit. Return it before booking again."),
        ALREADY_HOLDING("You already have a cycle booked or in use. Return it before booking another."),
        WEEKLY_LIMIT("You have used all 3 bookings for this week (rolling 7 days).");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    static EligibilityResult allow() {
        return new EligibilityResult(true, Reason.OK);
    }

    static EligibilityResult deny(Reason reason) {
        return new EligibilityResult(false, reason);
    }
}
