//package hello.booktown.service;
//
//import hello.booktown.domain.Book;
//import hello.booktown.domain.Scene;
//import hello.booktown.repository.BookRepository;
//import hello.booktown.repository.BookSummaryRepository;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.stereotype.Service;
//import org.springframework.util.FileCopyUtils;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class SummaryService {
//
//    @Value("classpath:/prompts/summary-prompt.st")
//    private Resource summaryPrompt;
//
//    private final RestTemplate restTemplate;
//    private final ChatClient chatClient; // GPT 요약용 커스텀 서비스
//    private final BookRepository bookRepository;
//    private final BookSummaryRepository bookSummaryRepository;
//    private final StabilityAIService stabilityAIService;
//
//    public SummaryService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder, BookRepository bookRepository, SceneRepository sceneRepository, BookSummaryRepository bookSummaryRepository, StabilityAIService stabilityAIService) {
//        this.restTemplate = restTemplate;
//        this.chatClient = chatClientBuilder.build();
//        this.bookRepository = bookRepository;
//        this.bookSummaryRepository = bookSummaryRepository;
//        this.stabilityAIService = stabilityAIService;
//    }
//
//    public void summarizeBookToSingleScene(Long bookId) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("📚 책을 찾을 수 없습니다."));
//
//        // 책 전문 가져오기
//        String fullText = restTemplate.getForObject(book.getSummaryUrl(), String.class);
//
//        // 전문을 2000자씩 청크 분할
//        List<String> chunks = splitTextIntoChunks(fullText, 2000);
//
//        // 각 청크에 대해 GPT 요약
//        List<String> summaries = new ArrayList<>();
//        for (String chunk : chunks) {
//            String prompt = buildPromptFromTemplate(chunk);
//            String summary = chatClient.prompt(prompt).call().content().trim();
//            summaries.add(summary);
//        }
//
//        // 요약된 모든 내용을 기반으로 최종 10개 문단 생성
//        String combined = String.join("\n\n", summaries);
//        String finalPrompt = buildFinalPrompt(combined);
//        String finalSummary = chatClient.prompt(finalPrompt).call().content().trim();
//
//        // 줄거리 10개 문단으로 분할
//        String[] paragraphs = finalSummary.split("\\n\\n");
//        List<Map<String, Object>> scenesList = new ArrayList<>();
//
//        for (int i = 0; i < paragraphs.length; i++) {
//            Map<String, Object> sceneObj = new LinkedHashMap<>();
//            sceneObj.put("chapter", "chapter-" + (i + 1));
//            sceneObj.put("content", paragraphs[i].trim());
//            sceneObj.put("illustrationUrl", null); // 필요시 Stability API 결과 저장
//            scenesList.add(sceneObj);
//        }
//
//        // JSON 직렬화 후 Scene에 저장
//        Scene scene = new Scene();
//        scene.setBook(book);
//        scene.setChapterId("summary-all"); // 전체 요약 구분용
//        scene.setContent(toJson(scenesList)); // @Lob 필드
//        sceneRepository.save(scene);
//    }
//
//    private List<String> splitTextIntoChunks(String text, int chunkSize) {
//        List<String> chunks = new ArrayList<>();
//        int length = text.length();
//        for (int i = 0; i < length; i += chunkSize) {
//            chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
//        }
//        return chunks;
//    }
//
//    private String buildPromptFromTemplate(String chunk) {
//        try {
//            String template = FileCopyUtils.copyToString(new InputStreamReader(
//                    summaryPrompt.getInputStream(), StandardCharsets.UTF_8));
//            return template.replace("{chunk}", chunk);
//        } catch (Exception e) {
//            throw new RuntimeException("📝 프롬프트 템플릿을 읽는 데 실패했습니다.", e);
//        }
//    }
//
//    private String buildFinalPrompt(String combinedSummaries) {
//        return """
//        당신은 책 내용을 교육적으로 요약하는 AI 어시스턴트입니다.
//        다음은 책의 요약 내용입니다.
//
//        이를 바탕으로 책의 전체 줄거리를 10개의 문단으로 분할하여 작성해주세요.
//        각 문단은 약 500자 분량으로 매우 구체적이고 길게 작성해야 하며, 줄거리만 보고도 원작을 충분히 이해할 수 있도록 서술형으로 작성합니다.
//        말투는 "~합니다", "~했습니다" 식의 공손한 형태를 유지해주세요.
//
//        각 문단은 \\n\\n으로 구분해 주세요.
//
//        전체 요약 내용:
//        %s
//        """.formatted(combinedSummaries);
//    }
//
//    private String toJson(List<Map<String, Object>> scenesList) {
//        try {
//            return new com.fasterxml.jackson.databind.ObjectMapper()
//                    .writerWithDefaultPrettyPrinter()
//                    .writeValueAsString(scenesList);
//        } catch (Exception e) {
//            throw new RuntimeException("❌ JSON 직렬화 실패", e);
//        }
//    }
//}
