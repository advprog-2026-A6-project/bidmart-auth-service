package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DummyUserService {
    @Autowired
    private DummyUserRepository repository;

    public List<DummyUser> getAllUsers() {
        return repository.findAll();
    }

    public DummyUser registerUser(String nama) {
        DummyUser user = new DummyUser();
        user.setNama(nama);
        return repository.save(user);
    }
}