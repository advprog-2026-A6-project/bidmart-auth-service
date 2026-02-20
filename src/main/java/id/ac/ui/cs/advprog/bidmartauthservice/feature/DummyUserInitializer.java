package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DummyUserInitializer implements CommandLineRunner {

    @Autowired
    private DummyUserRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            DummyUser user1 = new DummyUser();
            user1.setNama("Admin Auth");
            repository.save(user1);
        }
    }
}
