package you.shall.not.pass.filter.staticresource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import you.shall.not.pass.domain.Access;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class HighLevelStaticResource implements StaticResourceValidator {

    private static final Logger LOG = LoggerFactory.getLogger(HighLevelStaticResource.class);

    @Value("classpath:static/Level2/**")
    private Resource[] level2;

    @Autowired
    private StaticResourceService staticResourceService;

    private List<String> staticResources;

    @PostConstruct
    public void setList() {
        staticResources = staticResourceService.resolveStaticResources(level2);
        LOG.info("{} level resources: {}", requires(), staticResources);
    }

    @Override
    public boolean isApplicable(String requestUri) {
        boolean isApplicable = staticResources.stream().
                anyMatch(s -> s.equalsIgnoreCase(requestUri));
        LOG.info("matches {} resource: {}", requires(), isApplicable);
        return isApplicable;
    }

    @Override
    public Access requires() {
        return Access.Level2;
    }
}
