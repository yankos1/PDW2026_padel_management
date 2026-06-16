package be.ephec.pdw.padel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PadelController {

    // TODO [BONUS] Remplacer cet endpoint de test par un vrai endpoint de health-check documente.
    @GetMapping("/test")
    public String index() {
        return "Hello World";
    }
}
