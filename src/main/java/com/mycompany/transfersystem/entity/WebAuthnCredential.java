package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "webauthn_credentials")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebAuthnCredential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "public_key_cose", length = 4096)
    private byte[] publicKeyCose;
    @Column(name = "sign_count")
    private long signCount;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
