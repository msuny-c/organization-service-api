package ru.itmo.organization.service.storage;

import java.io.InputStream;

public record StorageStream(
        InputStream inputStream,
        String fileName,
        String contentType
) {}
