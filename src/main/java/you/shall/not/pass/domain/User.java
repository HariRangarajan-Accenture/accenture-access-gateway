package you.shall.not.pass.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class User {
    @Id
    private String id;
    private String userName;
    private char[] level1Password;
    private char[] level2Password;
}
