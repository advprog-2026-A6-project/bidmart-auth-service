package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dummy_users")
@Getter
@Setter
public class DummyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nama;
}