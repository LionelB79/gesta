package fr.gesta;

import fr.gesta.squelette.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    public record PongResponse(String pong){}

    @GetMapping("/api/test/ping")
    public PongResponse pong() {
        return new PongResponse("pong");
    }

    @GetMapping("/api/test/boom")
    public String err() {

            throw new ApiException("Paramètre introuvable",HttpStatus.NOT_FOUND,"TEST_BOOM");

    }


}
