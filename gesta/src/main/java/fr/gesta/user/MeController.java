package fr.gesta.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/me")
    public MeResponse me(Authentication  authentication) {
        return new MeResponse(authentication.getName());
    }
}
