package be.ephec.pdw.padel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PadelController {

    @GetMapping("/test")
    public String index() {
        return "Hello World";
    }
}
