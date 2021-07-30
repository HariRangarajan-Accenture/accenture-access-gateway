package you.shall.not.pass.domain;

import java.util.Arrays;
import java.util.Optional;

public enum Access {
    Level1(1),
    Level2(2);

    private int level;

    Access(int level) {
        this.level = level;
    }

    public static Optional<Access> find(String lvl) {
        return Arrays.stream(Access.values()).filter(gateKeeperGrant ->
                gateKeeperGrant.level == Integer.valueOf(lvl)).findFirst();
    }

    public boolean levelIsHigher(Access sessionAccess) {
        return sessionAccess == null
                || this.level > sessionAccess.level;
    }
}
