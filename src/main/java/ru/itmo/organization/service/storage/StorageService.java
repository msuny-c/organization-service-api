package ru.itmo.organization.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.config.MinioProperties;
import ru.itmo.organization.exception.StorageUnavailableException;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long DEFAULT_PART_SIZE = 10 * 1024 * 1024;
    private static final String STORAGE_UNAVAILABLE_MESSAGE =
            "Хранилище временно недоступно. Повторите попытку позже.";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public StorageTransaction stageImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл отсутствует или пуст");
        }
        ensureBucket();

        String objectId = UUID.randomUUID().toString().replace("-", "");
        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        String tempObjectName = "imports/staged/" + objectId;
        String finalObjectName = "imports/" + objectId + "/" + sanitizedName;
        long size = file.getSize();
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(tempObjectName)
                    .stream(inputStream, size > 0 ? size : -1, size > 0 ? -1 : DEFAULT_PART_SIZE)
                    .contentType(contentType);
            minioClient.putObject(builder.build());
            return new StorageTransaction(
                    properties.getBucket(),
                    tempObjectName,
                    finalObjectName,
                    sanitizedName,
                    size,
                    contentType
            );
        } catch (Exception ex) {
            throw wrapStorageException("Не удалось сохранить файл в хранилище", ex);
        }
    }

    public void commit(StorageTransaction transaction) {
        if (transaction == null) {
            return;
        }
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(transaction.bucket())
                    .object(transaction.finalObjectName())
                    .source(CopySource.builder()
                            .bucket(transaction.bucket())
                            .object(transaction.tempObjectName())
                            .build())
                    .build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(transaction.bucket())
                    .object(transaction.tempObjectName())
                    .build());
        } catch (Exception ex) {
            throw wrapStorageException("Не удалось зафиксировать файл в хранилище", ex);
        }
    }

    public void rollback(StorageTransaction transaction) {
        if (transaction == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(transaction.bucket())
                    .object(transaction.tempObjectName())
                    .build());
        } catch (Exception ignored) {
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(transaction.bucket())
                    .object(transaction.finalObjectName())
                    .build());
        } catch (Exception ignored) {
        }
    }

    public StorageStream load(String bucket, String objectName, String fallbackName, String contentType) {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            return new StorageStream(stream, fallbackName, contentType);
        } catch (Exception ex) {
            if (isConnectionIssue(ex)) {
                throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, ex);
            }
            throw new IllegalArgumentException("Файл не найден в хранилище");
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
            }
        } catch (Exception ex) {
            throw wrapStorageException("Не удалось подготовить бакет хранилища", ex);
        }
    }

    private String sanitizeFileName(String original) {
        if (!StringUtils.hasText(original)) {
            return "import.json";
        }
        String cleaned = original.replace("\\", "_").replace("/", "_");
        if (cleaned.length() > 120) {
            return cleaned.substring(cleaned.length() - 120);
        }
        return cleaned;
    }

    private RuntimeException wrapStorageException(String message, Exception ex) {
        if (isConnectionIssue(ex)) {
            return new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, ex);
        }
        return new IllegalStateException(message, ex);
    }

    private boolean isConnectionIssue(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("failed to connect")
                        || lower.contains("connection refused")
                        || lower.contains("connect timed out")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
