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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    public SummaryService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder, BookRepository bookRepository, BookSummaryRepository bookSummaryRepository, SummarySceneRepository summarySceneRepository, StabilityAIService stabilityAIService, UserRepository userRepository) {
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

    @Value("classpath:/prompts/scene-prompt.st")
    private Resource scenePromptResource;

    public void summarizeBookForUser(Long userId, Long bookId) throws IOException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // URL에서 리다이렉트를 따라가고 최종 텍스트를 가져오기
        String bookText = fetchBookText(book.getSummaryUrl());

        System.out.println(bookText);

        // 텍스트를 청크로 나누기
        List<String> chunks = splitTextIntoChunks(bookText, 2000);
        String combinedSummary = summarizeChunks(chunks);

        // GPT에 요약 요청하기
        String summaryPrompt = readPrompt(summaryPromptResource);
        String finalPrompt = summaryPrompt
                .replace("{{title}}", book.getTitle())
                .replace("{{summary}}", combinedSummary);

        String fullSummary = chatClient.prompt(finalPrompt).call().content().trim();
        String[] paragraphs = fullSummary.split("\\n\\n");

        // 요약 저장
        BookSummary bookSummary = new BookSummary();
        bookSummary.setUser(user);
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        // 씬 저장
        for (int i = 0; i < paragraphs.length; i++) {
            String content = paragraphs[i].trim();

            String scenePrompt = readPrompt(scenePromptResource)
                    .replace("{{title}}", book.getTitle())
                    .replace("{{content}}", content);

            String imageUrl = stabilityAIService.generateSceneImage(scenePrompt, userId, book.getTitle(), i + 1);

            SummaryScene scene = new SummaryScene();
            scene.setBookSummary(bookSummary);
            scene.setPageNumber(i + 1);
            scene.setContent(content);
            scene.setIllustrationUrl(imageUrl);

            summarySceneRepository.save(scene);
        }
    }

    private String summarizeChunks(List<String> chunks) {
        StringBuilder combined = new StringBuilder();
        for (String chunk : chunks) {
            String prompt = "다음 책 내용을 한국어로 요약해 주세요. 서론이나 결론은 제외하고 줄거리만 요약해 주세요:\n\n" + chunk;
            String summary = chatClient.prompt(prompt).call().content().trim();
            combined.append(summary).append("\n\n");
        }
        return combined.toString();
    }

    private String readPrompt(Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // URL에서 텍스트 가져오기 (리다이렉트를 자동으로 처리)
    private String fetchBookText(String initialUrl) throws IOException {
        // Jsoup을 사용하여 URL을 가져오기
        Document doc = Jsoup.connect(initialUrl).followRedirects(true).get();
        return doc.text(); // 텍스트만 추출
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

        return summarySceneRepository.findByBookSummary(bookSummary).stream()
                .map(scene -> new SummarySceneResponse(
                        scene.getPageNumber(),
                        scene.getContent(),
                        scene.getIllustrationUrl()
                ))
                .toList();
    }
}
