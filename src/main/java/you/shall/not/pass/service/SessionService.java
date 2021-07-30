package you.shall.not.pass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import you.shall.not.pass.domain.Access;
import you.shall.not.pass.domain.Session;
import you.shall.not.pass.domain.User;
import you.shall.not.pass.repositories.SessionRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;

import static you.shall.not.pass.filter.SecurityFilter.SESSION_COOKIE;

@Service
public class SessionService {

    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CsrfProtectionService csrfProtectionService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private DateService dateService;

    @Value("${session.expiry.seconds}")
    private int sessionExpirySeconds;

    public Optional<Session> findSessionByToken(String token) {
        Example<Session> example = Example.of(Session.builder()
                .token(token).build());
        return sessionRepository.findOne(example);
    }

    public boolean isExpiredSession(Optional<Session> optionalSession) {
        return !optionalSession.isPresent() || optionalSession.filter(session -> LocalDateTime.now()
                .isAfter(dateService.asLocalDateTime(session.getDate()))).isPresent();
    }

    private Optional<Session> findLastKnownSession(User user, Access grant) {
        Example<Session> example = Example.of(Session.builder()
                .userId(user.getId()).grant(grant).build());
        return sessionRepository.findAll(example).stream()
                .sorted(Comparator.comparing(Session::getDate,
                Comparator.nullsLast(Comparator.reverseOrder()))).findFirst();
    }

    public Optional<String> authenticatedSession() {
        final String username = LogonUserService.getCurrentUser().orElseThrow(()
                -> new RuntimeException("unknown user requesting session!"));
        final Access level = LogonUserService.getCurrentAccessLevel().orElseThrow(()
                -> new RuntimeException("Invalid user access level!"));

        final User user = userService.getUserByName(username);
        Optional<Session> priorSession = findLastKnownSession(user, level);

        boolean expired = isExpiredSession(priorSession);
        if (!expired) {
            LOG.info("returning old session cookie");
            return createOldSessionCookie(priorSession);
        }

        LOG.info("returning new session cookie");
        return createNewSessionCookie(level, user);
    }

    private Optional<String> createOldSessionCookie(Optional<Session> priorSession) {
        Session session = priorSession.orElseThrow(()
                -> new RuntimeException("This should never happen you may not pass!"));
        LocalDateTime cookieDate = dateService.asLocalDateTime(session.getDate());
        long diff = LocalDateTime.now().until(cookieDate, ChronoUnit.SECONDS);
        return Optional.of(createCookie(session.getToken(), (int) diff));
    }

    private Optional<String> createNewSessionCookie(Access grant, User user) {
        final String token = csrfProtectionService.generateToken();

        Session session = Session.builder()
                .date(dateService.asDate(LocalDateTime.now().plusSeconds(sessionExpirySeconds)))
                .grant(grant)
                .token(token)
                .userId(user.getId())
                .build();

        sessionRepository.save(session);
        return Optional.of(createCookie(token, sessionExpirySeconds));
    }

    private String createCookie(String token, int expireInSeconds) {
        return cookieService.createCookie(SESSION_COOKIE, token, expireInSeconds);
    }

}
