package you.shall.not.pass.controller;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import you.shall.not.pass.dto.StaticResources;
import you.shall.not.pass.filter.staticresource.StaticResourceService;
import you.shall.not.pass.service.CookieService;
import you.shall.not.pass.service.CsrfProtectionService;
import you.shall.not.pass.dto.Success;
import you.shall.not.pass.service.SessionService;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Controller
public class GateController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CsrfProtectionService csrfProtectionService;

    @Autowired
    private StaticResourceService resourceService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private Gson gson;

    @GetMapping({"/access"})
    public ResponseEntity<String> access(HttpServletResponse response) {
        Success.SuccessBuilder builder = Success.builder();
        Optional<String> optionalSession = sessionService.authenticatedSession();
        optionalSession.ifPresent(session -> {
            String csrf = csrfProtectionService.getCsrfCookie();
            cookieService.addCookie(csrf, response);
            cookieService.addCookie(session, response);
            builder.authenticated(true);
        });
        return ResponseEntity.ok(gson.toJson(builder.build()));
    }

    @GetMapping({"/resources"})
    public ResponseEntity<String> resources() {
        StaticResources resources = StaticResources.builder()
                .resources(resourceService.getAllStaticResources()).build();
        return ResponseEntity.ok(gson.toJson(resources));
    }

    @GetMapping({"/home"})
    public String hello() {
        return "any-app";
    }

}
