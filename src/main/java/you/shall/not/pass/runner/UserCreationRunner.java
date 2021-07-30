package you.shall.not.pass.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import you.shall.not.pass.domain.User;
import you.shall.not.pass.properties.UserProperties;
import you.shall.not.pass.repositories.UserRepository;

import java.util.Optional;

@Component
public class UserCreationRunner implements ApplicationRunner {

    @Autowired
    private UserRepository resp;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserProperties userProperties;

    private static final Logger LOG = LoggerFactory.getLogger(UserCreationRunner.class);


    @Override
    public void run(ApplicationArguments applicationArguments) {
        try {
            for (UserProperties.User newUser : userProperties.getUsers()) {
                User.UserBuilder builder = User.builder();

                builder.userName(newUser.getUserName())
                        .level1Password(
                        passwordEncoder.encode(
                                String.valueOf(newUser.getLevel1Password())).toCharArray())
                        .level2Password(
                        passwordEncoder.encode(
                                String.valueOf(newUser.getLevel2Password())).toCharArray());

                Example<User> example = Example.of(User.builder().userName(newUser.getUserName()).build());
                Optional<User> OptionalUser = resp.findOne(example);

                OptionalUser.ifPresent(user -> {
                    builder.id(user.getId());
                });

                User saved = resp.save(builder.build());
                LOG.info("User {} created for {}...",saved.getId() , newUser.getUserName());
            }
        } catch (Exception ex) {
            LOG.info("Error running system init", ex);
            throw ex;
        }
    }
}