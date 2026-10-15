package fr.gesta.auth;

import fr.gesta.exception.InvalidCredentialsException;
import fr.gesta.user.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody AuthRequest authRequest, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext s = SecurityContextHolder.createEmptyContext();
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(authRequest.username(), authRequest.password());
        Authentication authenticationResponse;
        try {
            authenticationResponse = this.authenticationManager.authenticate(authenticationRequest);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        s.setAuthentication(authenticationResponse);
        SecurityContextHolder.setContext(s);
        securityContextRepository.saveContext(s, request, response);
        return new MeResponse(authenticationResponse.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
