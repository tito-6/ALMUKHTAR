package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "fee_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String governorate;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 255)
    private String street;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
