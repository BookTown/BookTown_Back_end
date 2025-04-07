package hello.booktown.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final AmazonS3 amazonS3Client;

    @Value("${S3_BUCKET_NAME}")
    private String bucketName;

    /**
     * 이미지 데이터를 S3에 업로드하고 URL 반환
     *
     * @param imageData 바이트 배열 이미지
     * @param fileName 저장할 파일명 (예: image_1712468890000.jpg)
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadImage(byte[] imageData, String fileName) {
        InputStream inputStream = new ByteArrayInputStream(imageData);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageData.length);
        metadata.setContentType("image/jpeg");

        amazonS3Client.putObject(bucketName, fileName, inputStream, metadata);

        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    }
}

