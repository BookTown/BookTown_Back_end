package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.SummaryScene;
import hello.booktown.domain.User;
import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.BookSummaryRepository;
import hello.booktown.repository.SummarySceneRepository;
import hello.booktown.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Service
public class SummaryService {

    private final BookRepository bookRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;
    private final RestTemplate restTemplate;
    private final ChatClient chatClient;
    private final StabilityAIService stabilityAIService;
    private final UserRepository userRepository;


    public SummaryService(RestTemplate restTemplate,
                          ChatClient.Builder chatClientBuilder,
                          BookRepository bookRepository,
                          BookSummaryRepository bookSummaryRepository,
                          SummarySceneRepository summarySceneRepository,
                          StabilityAIService stabilityAIService, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.summarySceneRepository = summarySceneRepository;
        this.stabilityAIService = stabilityAIService;
        this.userRepository = userRepository;
    }

    @Value("classpath:/prompts/summary-prompt.st")
    private Resource summaryPromptResource;

    public void summarizeBookForUser(Long userId, Long bookId) throws IOException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        String bookText = restTemplate.getForObject(book.getSummaryUrl(), String.class);
        List<String> chunks = splitTextIntoChunks(bookText, 2000);

        StringBuilder combined = new StringBuilder();
        for (String chunk : chunks) {
            String chunkPrompt = "다음 책 내용을 한국어로 요약해 주세요:\n\n" + chunk;
            String chunkSummary = chatClient.prompt(chunkPrompt).call().content().trim();
            combined.append(chunkSummary).append("\n\n");
        }

        String template = new String(summaryPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String finalPrompt = template.replace("{{summary}}", combined.toString());

        String fullSummary = chatClient.prompt(finalPrompt).call().content().trim();
        String[] paragraphs = fullSummary.split("\\n\\n");

        BookSummary bookSummary = new BookSummary();
        bookSummary.setUser(user);
        bookSummary.setBook(book);
        bookSummary.setFullSummary(fullSummary);
        bookSummaryRepository.save(bookSummary);

        for (int i = 0; i < paragraphs.length; i++) {
            String content = paragraphs[i].trim();
            String imagePrompt = content + "\n\n위 내용을 묘사한 일러스트를 생성해 주세요.";
            String imageUrl = stabilityAIService.generateSceneImage(imagePrompt, userId, book.getTitle(), i + 1);

            SummaryScene scene = new SummaryScene();
            scene.setBookSummary(bookSummary);
            scene.setPageNumber(i + 1);
            scene.setContent(content);
            scene.setIllustrationUrl(imageUrl);

            summarySceneRepository.save(scene);
        }
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    public List<SummarySceneResponse> getSummaryScenes(Long userId, Long bookId) {
        BookSummary bookSummary = bookSummaryRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));

        List<SummaryScene> scenes = summarySceneRepository.findByBookSummary(bookSummary);

        List<SummarySceneResponse> responseList = new ArrayList<>();
        for (SummaryScene scene : scenes) {
            responseList.add(new SummarySceneResponse(
                    scene.getPageNumber(),
                    scene.getContent(),
                    scene.getIllustrationUrl()
            ));
        }

        return responseList;
    }

}
