package in.ac.iitm.cs5013.cyclebooking.idverify;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import org.junit.jupiter.api.Test;

/** ID Verification test: the cross-check between what the card says and who is logged in. */
class DefaultIdVerificationServiceTest {

    private final IdVerificationService service = new DefaultIdVerificationService("");
    private final ResidentIdentity yashas = new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in");

    @Test
    void matchesLogin_acceptsOwnRollNumberReadConfidently() {
        assertThat(service.matchesLogin(new ExtractedIdentity("Y K", "CE24B128", "Jamuna", 91f), yashas)).isTrue();
    }

    @Test
    void matchesLogin_acceptsExactlyTheConfidenceThreshold() {
        float threshold = IdVerificationService.MIN_CONFIDENCE;
        assertThat(service.matchesLogin(new ExtractedIdentity(null, "CE24B128", null, threshold), yashas)).isTrue();
        assertThat(service.matchesLogin(new ExtractedIdentity(null, "CE24B128", null, threshold - 0.1f), yashas))
                .isFalse();
    }

    @Test
    void matchesLogin_rejectsSomeoneElsesCardEvenWhenReadConfidently() {
        assertThat(service.matchesLogin(new ExtractedIdentity("A G", "CE24B001", "Jamuna", 95f), yashas)).isFalse();
    }

    @Test
    void matchesLogin_rejectsCardWithNoRollNumberRead() {
        assertThat(service.matchesLogin(ExtractedIdentity.unreadable(), yashas)).isFalse();
    }

    @Test
    void scanCard_returnsUnreadableForBytesThatAreNotAPhoto() {
        ExtractedIdentity result = service.scanCard("definitely not a jpeg".getBytes());

        assertThat(result).isEqualTo(ExtractedIdentity.unreadable());
        assertThat(service.matchesLogin(result, yashas)).isFalse();
    }
}
