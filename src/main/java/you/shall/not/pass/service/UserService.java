package you.shall.not.pass.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import you.shall.not.pass.domain.User;
import you.shall.not.pass.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public User getUserByName(String name) {
        Example<User> example = Example.of(User.builder().userName(name).build());
        Optional<User> OptionalUser = repository.findOne(example);
        User user = OptionalUser.orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user;
    }

}
