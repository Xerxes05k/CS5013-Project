package in.ac.iitm.cs5013.cyclebooking.rules;

/**
 * Rule Engine: the hostel's own booking rules, kept apart from the booking flow so
 * they can be tested in isolation and, later, overridden by the GS (ADR 0003).
 */
public interface RuleService {

    EligibilityResult checkEligibility(long residentId);
}
