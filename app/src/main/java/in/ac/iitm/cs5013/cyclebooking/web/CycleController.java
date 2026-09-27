package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Week 1: read-only availability. Booking itself arrives in week 2, once the Rule
 * Engine and the ID-verification cross-check exist to gate it.
 */
@Controller
public class CycleController {

    private final CycleRepository cycles;

    public CycleController(CycleRepository cycles) {
        this.cycles = cycles;
    }

    @GetMapping("/cycles")
    String availability(Model model) {
        List<Cycle> all = cycles.findAllByOrderByLabelAsc();
        model.addAttribute("cycles", all);
        model.addAttribute("availableCount", all.stream().filter(c -> c.getState() == CycleState.AVAILABLE).count());
        return "cycles";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
