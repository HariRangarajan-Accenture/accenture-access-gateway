package you.shall.not.pass.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import you.shall.not.pass.exception.CsrfViolationException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class CsrfProtectionService {

    private static final Logger LOG = LoggerFactory.getLogger(CsrfProtectionService.class);

    private final static SecureRandom SECURE_RANDOM = new SecureRandom();

    private final static String CSRF_COOKIE_NAME = "CSRF";
    private final static String XSRF_GUARD_NAME = "XSRF";
    private final static int STANDARD_SIZE_TOKEN = 16;
    private final static int CSRF_TOKEN_SIZE = 8;
    private final static Pattern GUARD_PATTERN = Pattern.compile("[a-zA-Z0-9]{16}_[0-9]{10}");

    @Value("${csrf.expiry.seconds}")
    private int expiry;

    @Autowired
    private CookieService cookieService;

    public String generateToken() {
        return generateToken(STANDARD_SIZE_TOKEN);
    }

    public String generateToken(int size) {
        byte[] buffer = new byte[size];
        SECURE_RANDOM.nextBytes(buffer);
        return DatatypeConverter.printHexBinary(buffer);
    }

    public String getCsrfCookie() {
        long epoch = OffsetDateTime.now().plusSeconds(expiry).toEpochSecond();
        final String token = generateToken(CSRF_TOKEN_SIZE) + "_" + epoch;
        return cookieService.createCookie(CSRF_COOKIE_NAME, token, expiry);
    }

    public void validateCsrfCookie(HttpServletRequest request) {
        final String xsrfGuard = getCsrfGuardCheckValue(request);
        final String csrf = cookieService.getCookieValue(request, CSRF_COOKIE_NAME);

        LOG.info("incoming csrf cookie: {}", csrf);
        LOG.info("incoming xsrf value: {}", xsrfGuard);

        if (csrf == null || xsrfGuard == null) {
            throw new CsrfViolationException("You may not pass you seem to be missing something!");
        }

        final Matcher matcher = GUARD_PATTERN.matcher(csrf);
        boolean matches = matcher.matches();

        LOG.info("csrf cookie pattern guard passed: {}", matches);

        if (!matches) {
            throw new CsrfViolationException("Dont try and fake your key, I know all!");
        }

        long diff = getEpochSecondsDiff(csrf);

        LOG.info("csrf cookie expiry in {} secs", diff);

        if (!csrf.equals(xsrfGuard) || diff <= 0) {
            throw new CsrfViolationException("Two of one, which one is not the same!");
        }
    }

    private String getCsrfGuardCheckValue(HttpServletRequest request) {
        String guardCheckValue = request.getHeader(XSRF_GUARD_NAME);
        if (guardCheckValue == null) {
            guardCheckValue = request.getParameter(XSRF_GUARD_NAME);
        }
        return guardCheckValue;
    }

    private long getEpochSecondsDiff(String cookieValue) {
        final String values[] = cookieValue.split("_");
        final String epochReceived = values[1];
        final Instant ofEpochSecond = Instant.ofEpochSecond(Long.parseLong(epochReceived));
        final OffsetDateTime ofInstant = OffsetDateTime.ofInstant(ofEpochSecond, ZoneId.systemDefault());
        return OffsetDateTime.now().until(ofInstant, ChronoUnit.SECONDS);
    }

}
