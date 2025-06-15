package hello.booktown.util;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
public class S3Uploader {

    private final AmazonS3Client amazonS3Client;

    @Value("${S3_BUCKET_NAME}")
    private String bucket;

    public S3Uploader(AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String filename = "profile/" + UUID.randomUUID() + ext;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3Client.putObject(
                    new PutObjectRequest(bucket, filename, file.getInputStream(), metadata)
            );

            return amazonS3Client.getUrl(bucket, filename).toString();

        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    public String upload(java.io.File file, String key) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType("audio/mpeg");

            amazonS3Client.putObject(
                new PutObjectRequest(bucket, key, fis, metadata)
            );

            return amazonS3Client.getUrl(bucket, key).toString();
        } catch (java.io.IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }

    public void delete(String fileUrl) {
        String key = fileUrl.substring(fileUrl.indexOf("profile/")); // 경로 추출
        amazonS3Client.deleteObject(bucket, key);
    }
}