package you.shall.not.pass.filter.staticresource;

import you.shall.not.pass.domain.Access;

public interface StaticResourceValidator {
    boolean isApplicable(String requestUri);
    Access requires();
    void setList();
}
