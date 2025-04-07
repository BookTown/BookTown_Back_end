package hello.booktown.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
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
    private String stylePreset;

    private static final String API_URL = "https://api.stability.ai/v2beta/stable-image/generate/sd3";

    public StabilityAIService(RestTemplate restTemplate, S3UploadService s3UploadService) {
        this.restTemplate = restTemplate;
        this.s3UploadService = s3UploadService;
    }

    public String generateImage(String prompt) {
        try {
            // 멀티파트 요청 바디 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("prompt", prompt);
            body.add("style_preset", stylePreset);
            body.add("width", width);
            body.add("height", height);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(List.of(MediaType.parseMediaType("image/*"))); // 정확히 image/*로 지정
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] imageBytes = response.getBody();
                String fileName = "image_" + System.currentTimeMillis() + ".jpg";

                return s3UploadService.uploadImage(imageBytes, fileName);
            } else {
                return "Error: " + response.getStatusCode();
            }

        } catch (Exception e) {
            return "Exception occurred: " + e.getMessage();
        }
    }
}
