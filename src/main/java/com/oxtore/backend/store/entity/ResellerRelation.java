package com.oxtore.backend.store.entity;

import com.oxtore.backend.store.enums.ResellerRelationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reseller_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResellerRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "source_store_id", nullable = false, updatable = false)
    private Store sourceStore;

    @ManyToOne @JoinColumn(name = "reseller_store_id", nullable = false, updatable = false)
    private Store resellerStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResellerRelationStatus status;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private Long requestedBy;

    @Column(name = "decided_by")
    private Long decidedBy;

    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;

    @PrePersist
    private void prePersist() {
        this.requestedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        if (this.status == ResellerRelationStatus.ACCEPTED || this.status == ResellerRelationStatus.DECLINED
                || this.status == ResellerRelationStatus.REVOKED) {
            this.decidedAt = LocalDateTime.now();
        }
    }
}
