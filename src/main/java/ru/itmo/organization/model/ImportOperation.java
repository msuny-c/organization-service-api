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

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(name = "added_count")
    private Integer addedCount;

    @Column(name = "filename")
    private String filename;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public void markSuccess(int added) {
        this.status = ImportStatus.SUCCESS;
        this.addedCount = added;
        this.finishedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = ImportStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.errorMessage = message;
        this.addedCount = null;
    }
}
