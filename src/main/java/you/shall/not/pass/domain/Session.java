package you.shall.not.pass.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Builder
@Data
public class Session {
    @Id
    private String sessionId;
    private Access grant;
    private String userId;
    private String token;
    private Date date;
}
