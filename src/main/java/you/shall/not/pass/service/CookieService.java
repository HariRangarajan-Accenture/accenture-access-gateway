package you.shall.not.pass.service;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CookieService {

    public String getCookieValue(HttpServletRequest req, String cookieName) {
        if (req.getCookies() == null) {
            return null;
        }
        return Arrays.stream(req.getCookies())
                .filter(c -> c.getName().equals(cookieName))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }


    public String createCookie(String name, String token, int expireInSeconds) {
        List<String> headerValues = new ArrayList<>();
        headerValues.add(name + "=" + token);
        headerValues.add("SameSite=Strict");
        headerValues.add("Path=/");
        headerValues.add("HttpOnly");
        headerValues.add("Max-Age=" + expireInSeconds);
        return headerValues.stream().collect(Collectors.joining("; "));
    }

    public void addCookie(String cookie, HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie);
    }

}
