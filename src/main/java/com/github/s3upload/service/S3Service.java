package com.github.s3upload.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.s3upload.config.S3ClientProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3ClientProperties s3ClientProperties;

    public List<String> getAllBucketNames() {
        return s3Client.listBuckets().buckets().stream()
                .map(bucket -> bucket.name())
                .toList();
    }

    public List<String> getAllBucketObjects() {
        return s3Client.listObjectsV2(builder -> builder.bucket(s3ClientProperties.getS3().getBucket()))
                .contents().stream()
                .map(object -> object.key())
                .toList();
    }

    public String uploadFile(MultipartFile file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3ClientProperties.getS3().getBucket())
                    .key(file.getOriginalFilename())
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .storageClass(StorageClass.STANDARD)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getInputStream().readAllBytes()));
            return file.getOriginalFilename() + " uploaded successfully.";
        } catch (Exception e) {
            return "Failed to upload " + file.getOriginalFilename() + " " + e.getMessage();
        }
    }

    public String multipartUploadFile(MultipartFile file) {
        String bucketName = s3ClientProperties.getS3().getBucket();
        String key = file.getOriginalFilename();

        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        CreateMultipartUploadResponse multipartUploadResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = multipartUploadResponse.uploadId();
        log.info("Upload ID: {}", uploadId);

        try {
            InputStream inputStream = file.getInputStream();
            int BUFFER_SIZE = 1024 * 1024 * 5, partNumber = 1, bytesRead = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            List<CompletedPart> completedParts = new ArrayList<>();

            while ((bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                log.info("Uploading part number: {}, bytes read: {}", partNumber, bytesRead);

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();

                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
                        RequestBody.fromByteBuffer(ByteBuffer.wrap(buffer, 0, bytesRead)));

                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());

                log.info("Part number: {} uploaded successfully. | {}", partNumber++, uploadPartResponse);
            }

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();


            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            String response = s3Client.completeMultipartUpload(completeMultipartUploadRequest).key();
            log.info("File uploaded successfully. | {}", response);

            return response;
        } catch (Exception e) {
            AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
            log.error("Multipart upload aborted. | {}", e.getMessage());

            return "Failed to upload " + key + " " + e.getMessage();
        }
    }
    public String updateFile(String fileName, MultipartFile file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3ClientProperties.getS3().getBucket())
                    .key(fileName)
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .storageClass(StorageClass.STANDARD)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getInputStream().readAllBytes()));
            return fileName + " updated successfully.";
        } catch (Exception e) {
            return "Failed to update " + fileName + " " + e.getMessage();
        }
    }

    public void deleteFile(String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3ClientProperties.getS3().getBucket())
                .key(fileName)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public String downloadFile(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3ClientProperties.getS3().getBucket())
                .key(fileName)
                .build();

        try {
            byte[] responseBytes = s3Client.getObject(getObjectRequest).readAllBytes();
            File file = new File(System.getProperty("user.dir") + "/" + fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(responseBytes);
            fileOutputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            return "Failed to download " + fileName + " " + e.getMessage();
        }
    }
}
