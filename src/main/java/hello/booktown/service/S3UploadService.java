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
    public String uploadBookThumbnail(byte[] imageData, String fileName, String bookName) {
        try {
            // 책 이름을 안전한 S3 경로용 문자열로 변환 (예: "The Great Gatsby" → "the-great-gatsby")
            String safeBookName = bookName.toLowerCase().replaceAll("[^a-z0-9]", "-");

            // S3 경로 구성: "the-great-gatsby/thumbnail.jpg"
            String key = safeBookName + "/" + fileName;

            InputStream inputStream = new ByteArrayInputStream(imageData);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageData.length);
            metadata.setContentType("image/jpeg");

            amazonS3Client.putObject(bucketName, key, inputStream, metadata);

            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        } catch (Exception e) {
            // 에러가 발생했을 경우 예외 로깅 및 null 또는 커스텀 예외 반환
            e.printStackTrace(); // 필요시 로깅 프레임워크로 대체
            throw new RuntimeException("책 썸네일 업로드 실패", e);
        }
    }

    public String uploadSceneImage(byte[] imageData,  Long bookId, int sceneNumber) {
        try {
            String fileName = "scene-" + sceneNumber + ".jpg";
            String key = bookId + "/" + fileName; // ✅ 수정: userId/bookId/scene-번호.jpg

            InputStream inputStream = new ByteArrayInputStream(imageData);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageData.length);
            metadata.setContentType("image/jpeg");

            amazonS3Client.putObject(bucketName, key, inputStream, metadata);

            return "https://" + bucketName + ".s3.amazonaws.com/" + key;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("씬 이미지 업로드 실패", e);
        }
    }


}

