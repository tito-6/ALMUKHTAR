package com.mycompany.transfersystem.service.storage;

import java.io.InputStream;

/**
 * Abstraction for storing and retrieving KYC document files (MinIO/S3 or local).
 */
public interface DocumentStorageService {

    /**
     * Store document bytes and return a reference path for later retrieval.
     */
    String store(String applicationId, String docType, String filename, byte[] content);

    /**
     * Retrieve document as bytes by reference.
     */
    byte[] retrieve(String fileReference);

    /**
     * Retrieve as stream for large files.
     */
    InputStream retrieveStream(String fileReference);
}
