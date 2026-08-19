package com.codeduels.common.AWS.service;

import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.problem.model.TestCase;
import io.awspring.cloud.s3.S3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public String generatePresignedUploadUrl(String objectKey) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/zip")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        log.info("Generated presigned upload URL for key {}", objectKey);
        return url;
    }

    public boolean doesObjectExist(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public String moveObject(String sourceKey, String destinationKey) {
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName).sourceKey(sourceKey)
                    .destinationBucket(bucketName).destinationKey(destinationKey)
                    .build());

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName).key(sourceKey)
                    .build());

            log.info("Moved S3 object {} → {}", sourceKey, destinationKey);
            return destinationKey;

        } catch (S3Exception e) {
            log.error("Failed to move S3 object {} → {}", sourceKey, destinationKey, e);
            throw new RuntimeException("Error moving S3 object", e);
        }
    }

    public List<TestCase> downloadAndParseTestCases(String s3Key) {
        Map<String, String> inputs = new HashMap<>();
        Map<String, String> outputs = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(
                s3Client.getObject(GetObjectRequest.builder()
                        .bucket(bucketName).key(s3Key).build()))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String fileName = entry.getName();
                if (fileName.startsWith("__MACOSX/") || fileName.contains("/._")
                        || fileName.endsWith(".DS_Store")) {
                    continue;                                    // macOS zip junk
                }

                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                if (fileName.endsWith(".in")) {
                    inputs.put(baseName, content.trim());
                } else if (fileName.endsWith(".out")) {
                    outputs.put(baseName, content.trim());
                }
                zis.closeEntry();
            }

        } catch (NoSuchKeyException e) {
            throw new RessourceNotFoundException("Test case file not found: " + s3Key);
        } catch (S3Exception | IOException e) {
            throw new RuntimeException("Failed to process test cases from S3: " + s3Key, e);
        }

        return inputs.keySet().stream()
                .sorted()
                .map(base -> new TestCase(inputs.get(base), outputs.getOrDefault(base, ""), null))
                .toList();
    }
}