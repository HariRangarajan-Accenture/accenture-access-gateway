package you.shall.not.pass.filter;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import you.shall.not.pass.exception.CsrfViolationException;
import you.shall.not.pass.service.CsrfProtectionService;
import you.shall.not.pass.domain.Access;
import you.shall.not.pass.domain.Session;
import you.shall.not.pass.dto.Violation;
import you.shall.not.pass.exception.AccessGrantException;
import you.shall.not.pass.filter.staticresource.StaticResourceValidator;
import you.shall.not.pass.service.CookieService;
import you.shall.not.pass.service.SessionService;

@Component
@Order(1)
public class SecurityFilter implements Filter {

    public static final String SESSION_COOKIE = "GRANT";
    public static final String EXECUTE_FILTER_ONCE = "you.shall.not.pass.filter";

    private static final Logger LOG = LoggerFactory.getLogger(SecurityFilter.class);

    @Autowired
    private Gson gson;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private List<StaticResourceValidator> resourcesValidators;

    @Autowired
    private CsrfProtectionService csrfProtectionService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request.getAttribute(EXECUTE_FILTER_ONCE) == null) {
                shallNotPassLogic((HttpServletRequest) request);
            }
            request.setAttribute(EXECUTE_FILTER_ONCE, true);
            chain.doFilter(request, response);
        } catch (AccessGrantException age) {
            LOG.warn("Access violation, {}", age.getMessage());
            processAccessGrantError((HttpServletResponse) response, age);
        } catch (CsrfViolationException cve) {
            LOG.warn("CSRF violation, {}", cve.getMessage());
            processCsrfViolation((HttpServletResponse) response, cve);
        }
    }

    private void processCsrfViolation(HttpServletResponse response, CsrfViolationException cve) {
        Violation violation = Violation.builder()
                .message(cve.getMessage())
                .csrfPassed(false)
                .build();

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        writeResponse(response, gson.toJson(violation));
    }

    private void processAccessGrantError(HttpServletResponse response, AccessGrantException age) {
        Violation violation = Violation.builder()
                .message(age.getMessage())
                .requiredAccess(age.getRequired())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        writeResponse(response, gson.toJson(violation));
    }

    private void shallNotPassLogic(HttpServletRequest request) {
        final String cookieValue = cookieService.getCookieValue( request, SESSION_COOKIE);
        final Optional<Session> sessionByToken = sessionService.findSessionByToken(cookieValue);
        final String requestedUri = request.getRequestURI();
        LOG.info("incoming request {} with token {}", requestedUri, cookieValue);
        final Access grant = sessionByToken.map(Session::getGrant).orElse(null);
        LOG.info("user grant level {}", grant);
        final Optional<StaticResourceValidator> resourceValidator = getValidator(requestedUri);
        resourceValidator.ifPresent(validator -> {
            LOG.info("resource validator enforced {}", validator.requires());
            if (sessionService.isExpiredSession(sessionByToken)
                    || validator.requires().levelIsHigher(grant)) {
                throw new AccessGrantException(validator.requires(), "invalid access level");
            }
            csrfProtectionService.validateCsrfCookie(request);
        });
    }

    private Optional<StaticResourceValidator> getValidator(String requestedUri) {
       return resourcesValidators.stream().filter(staticResourceValidator
               -> staticResourceValidator.isApplicable(requestedUri)).findFirst();
    }

    private void writeResponse(HttpServletResponse response, String message) {
        try {
            PrintWriter out = response.getWriter();
            LOG.info("response message {}", message);
            out.print(message);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
