package ru.itmo.organization.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_operation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "ru.itmo.organization.model.ImportOperation")
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
    
    @Column(name = "storage_bucket")
    private String storageBucket;
    
    @Column(name = "storage_object")
    private String storageObject;
    
    @Column(name = "storage_filename")
    private String storageFileName;
    
    @Column(name = "storage_content_type")
    private String storageContentType;
    
    @Column(name = "storage_size")
    private Long storageSize;

    public void markSuccess(int added) {
        this.status = ImportStatus.SUCCESS;
        this.addedCount = added;
    }

    public void markFailed(String message) {
        this.status = ImportStatus.FAILED;
        this.addedCount = null;
        this.storageBucket = null;
        this.storageObject = null;
        this.storageFileName = null;
        this.storageContentType = null;
        this.storageSize = null;
    }
}
