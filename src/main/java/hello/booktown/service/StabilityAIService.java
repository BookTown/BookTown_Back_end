package hello.booktown.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class StabilityAIService {

    private final RestTemplate restTemplate;
    private final S3UploadService s3UploadService;

    @Value("${spring.ai.stabilityai.api-key}")
    private String apiKey;

    @Value("${spring.ai.stabilityai.image.options.width}")
    private int width;

    @Value("${spring.ai.stabilityai.image.options.height}")
    private int height;

    @Value("${spring.ai.stabilityai.image.options.style-preset}")
    private String defaultStylePreset;

    private static final String API_URL = "https://api.stability.ai/v2beta/stable-image/generate/sd3";

    public StabilityAIService(RestTemplate restTemplate, S3UploadService s3UploadService) {
        this.restTemplate = restTemplate;
        this.s3UploadService = s3UploadService;
    }

    public String generateThumbnail(String prompt, String bookName) {
        return generateImage(prompt, "analog-film", width, height, bookName, null);
    }

    public String generateSceneImage(String prompt, Long bookId, int sceneNumber) {
        return generateImage(prompt, defaultStylePreset, width, height, null, new SceneImageTarget(bookId, sceneNumber));
    }

    private String generateImage(String prompt, String stylePreset, int width, int height, String bookName, SceneImageTarget target) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("prompt", prompt);
            body.add("style_preset", stylePreset);
            body.add("width", width);
            body.add("height", height);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(List.of(MediaType.parseMediaType("image/*")));
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] imageBytes = response.getBody();
                if (target != null) {
                    return s3UploadService.uploadSceneImage(imageBytes, target.bookId(), target.sceneNumber());
                } else {
                    String fileName = "thumbnail_" + System.currentTimeMillis() + ".jpg";
                    return s3UploadService.uploadBookThumbnail(imageBytes, fileName, bookName);
                }
            } else {
                return "Error: " + response.getStatusCode();
            }

        } catch (Exception e) {
            return "Exception occurred: " + e.getMessage();
        }
    }

    private record SceneImageTarget(Long bookId, int sceneNumber) {}
}
