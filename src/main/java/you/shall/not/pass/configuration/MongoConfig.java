package you.shall.not.pass.configuration;

import com.mongodb.client.MongoClients;
import cz.jirutka.spring.embedmongo.EmbeddedMongoFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;


@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String name;

    @Bean
    public MongoDbFactory mongoDbFactory() {
        if (port == 0) {
            throw new RuntimeException("No port provided for mongo db, failed connection to db!");
        }

        if (host == null) {
            throw new RuntimeException("No host provided for mongo db, failed connection to db!");
        }

        String connectionURL = "mongodb://"+ host+":" + port;
        return new SimpleMongoClientDbFactory(MongoClients.create(connectionURL), name);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory);
        return mongoTemplate;
    }

    @Bean
    public void setupMongo()  {
        EmbeddedMongoFactoryBean mongo = new EmbeddedMongoFactoryBean();
        mongo.setBindIp(host);
        mongo.setPort(port);
    }

}
