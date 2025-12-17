package ru.itmo.organization.service.storage;

public record StorageTransaction(
        String bucket,
        String tempObjectName,
        String finalObjectName,
        String originalFileName,
        long size,
        String contentType
) {}
