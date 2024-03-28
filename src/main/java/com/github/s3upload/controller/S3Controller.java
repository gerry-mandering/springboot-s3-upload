package com.github.s3upload.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.s3upload.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @GetMapping("/buckets/list")
    public List<String> getAllBucketNames() {
        return s3Service.getAllBucketNames();
    }

    @GetMapping("/bucket/all")
    public List<String> getAllBucketObjects() {
        return s3Service.getAllBucketObjects();
    }

    @PostMapping("/bucket")
    public List<String> uploadFile(@RequestParam("file") List<MultipartFile> files) {
        return files.stream()
                .map(file -> s3Service.uploadFile(file))
                .collect(Collectors.toList());
    }

    @PostMapping("/bucket/multipart")
    public List<String> multipartUploadFile(@RequestParam("file") List<MultipartFile> files) {
        log.info("Multipart Upload Started.");
        return files.stream()
                .map(file -> s3Service.multipartUploadFile(file))
                .collect(Collectors.toList());
    }

    @PutMapping("/bucket")
    public String updateFile(@RequestParam("fileName") String fileName, @RequestParam("file") MultipartFile file) {
        return s3Service.updateFile(fileName, file);
    }

    @DeleteMapping("/bucket")
    public void deleteFile(@RequestParam("fileName") String fileName) {
        s3Service.deleteFile(fileName);
    }

    @PostMapping("/bucket/download")
    public String downloadFile(@RequestParam("fileName") String fileName) {
        return s3Service.downloadFile(fileName);
    }
}
