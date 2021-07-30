package you.shall.not.pass.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import you.shall.not.pass.domain.Session;

public  interface SessionRepository extends MongoRepository<Session, String> {
}
