package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.Scene;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.SceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SceneService {

    private final RestTemplate restTemplate;
    private final ChatClient chatClient; // GPT 요약용 커스텀 서비스
    private final BookRepository bookRepository;
    private final SceneRepository sceneRepository;
    private final StabilityAIService stabilityAIService;

    public SceneService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder, BookRepository bookRepository, SceneRepository sceneRepository, StabilityAIService stabilityAIService) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.sceneRepository = sceneRepository;
        this.stabilityAIService = stabilityAIService;
    }

    public void summarizeBookByChunks(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("📚 책을 찾을 수 없습니다."));

        // 1. 전체 텍스트 가져오기
        String fullText = restTemplate.getForObject(book.getSummaryUrl(), String.class);

        // 2. 텍스트를 2000자 단위 청크로 나누기
        List<String> chunks = splitTextIntoChunks(fullText, 2000);

        StringBuilder combinedSummaries = new StringBuilder();

        // 3. 각 청크 요약
        for (String chunk : chunks) {
            String chunkPrompt = """
            다음 글의 내용을 간단하게 한국어로 요약해 주세요:

            %s
            """.formatted(chunk);

            String summary = chatClient.prompt(chunkPrompt)
                    .call()
                    .content()
                    .trim();

            combinedSummaries.append(summary).append("\n\n");
        }

        // 4. 전체 요약문을 다시 10문단으로 재요약
        String finalPrompt = """
        다음 내용을 기반으로 책의 전체 줄거리를 10개의 문단으로 요약해 주세요.
        각 문단은 줄바꿈(\\n\\n)으로 구분해 주세요. 한국어로 작성해 주세요.

        전체 요약 내용:
        %s
        """.formatted(combinedSummaries.toString());

        String finalSummary = chatClient.prompt(finalPrompt)
                .call()
                .content()
                .trim();

        // 5. 문단 분할 및 Scene 저장
        String[] paragraphs = finalSummary.split("\\n\\n");

        for (int i = 0; i < paragraphs.length; i++) {
            Scene scene = new Scene();
            scene.setChapterId("chapter-" + (i + 1));
            scene.setBook(book);
            scene.setContent(paragraphs[i].trim());
            // 그림 추가 필요 시 여기서 Stability API 연동 가능
            sceneRepository.save(scene);
        }
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
        }
        return chunks;
    }
}
