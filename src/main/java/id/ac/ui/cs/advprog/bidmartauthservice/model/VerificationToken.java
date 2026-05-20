package id.ac.ui.cs.advprog.bidmartauthservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationMethod method;

    @Column(name = "device_id")
    private String deviceId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean consumed = false;
}
