package ru.itmo.organization.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_operation")
public class ImportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false)
    private ImportObjectType objectType;

    @Column(name = "added_count")
    private Integer addedCount;

    public void markSuccess(int added) {
        this.status = ImportStatus.SUCCESS;
        this.addedCount = added;
    }

    public void markFailed(String message) {
        this.status = ImportStatus.FAILED;
        this.addedCount = null;
    }
}
