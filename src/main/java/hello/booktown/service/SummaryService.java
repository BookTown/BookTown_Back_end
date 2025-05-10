package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.SummaryScene;
import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.BookSummaryRepository;
import hello.booktown.repository.SummarySceneRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class SummaryService {

    private final BookRepository bookRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;
    private final RestTemplate restTemplate;
    private final ChatClient chatClient;
    private final StabilityAIService stabilityAIService;

    @Value("classpath:/prompts/summary-prompt.st")
    private Resource summaryPrompt;

    @Value("classpath:/prompts/scene-prompt.st")
    private Resource scenePrompt;

    public SummaryService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder,
                          BookRepository bookRepository, BookSummaryRepository bookSummaryRepository,
                          SummarySceneRepository summarySceneRepository, StabilityAIService stabilityAIService) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.summarySceneRepository = summarySceneRepository;
        this.stabilityAIService = stabilityAIService;
    }

    public List<SummarySceneResponse> summarizeBook(Long bookId) throws IOException {
        if (bookSummaryRepository.existsByBookId(bookId)) {
            return getSummaryScenes(bookId);  // 이미 요약이 존재하면 반환
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        String bookText = fetchBookText(book.getSummaryUrl());
        List<String> chunkSummaries = summarizeChunksAsync(bookText);
        String fullSummary = String.join("\n\n", chunkSummaries);
        List<String> sceneSummaries = summarizeInto10Scenes(fullSummary);

        saveScenesParallel(book, sceneSummaries);

        return getSummaryScenes(bookId);  // 요약 완료 후 결과 반환
    }



    private String fetchBookText(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .timeout(60000)
                .followRedirects(true)
                .get();
        return doc.text();
    }

    private List<String> summarizeChunksAsync(String text) {
        List<String> chunks = splitTextIntoChunks(text, 8000);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunks.size(), 10));
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String chunk : chunks) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                String prompt = "다음 내용을 요약해줘:\n\n" + chunk;
                return chatClient.prompt(prompt).call().content().trim();
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private List<String> summarizeInto10Scenes(String fullSummary) {
        String prompt = loadPromptTemplate(summaryPrompt).replace("{{fullSummary}}", fullSummary);
        String result = chatClient.prompt(prompt).call().content().trim();
        List<String> scenes = List.of(result.split("\\n\\n"));

        List<String> valid = scenes.stream().filter(s -> s.length() > 100).toList();
        if (valid.size() >= 10) return valid.subList(0, 10);

        List<String> padded = new ArrayList<>(valid);
        while (padded.size() < 10) padded.add("");
        return padded;
    }

    private void saveScenesParallel(Book book, List<String> sceneSummaries) {
        BookSummary bookSummary = new BookSummary();
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<SceneResult>> futures = new ArrayList<>();

        for (int i = 0; i < sceneSummaries.size(); i++) {
            int pageNumber = i + 1;
            String content = sceneSummaries.get(i).trim();
            if (content.isEmpty()) continue;

            int finalPageNumber = pageNumber;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    String translationPrompt = "Translate the following Korean scene summary into fluent English. Return only the translated text, no explanation:\n\n" + content;
                    String translated = chatClient.prompt(translationPrompt).call().content().trim();
                    String scenePromptText = loadPromptTemplate(scenePrompt).replace("{{scene}}", translated);
                    String imageUrl = stabilityAIService.generateSceneImage(scenePromptText, book.getId(), finalPageNumber);
                    return new SceneResult(finalPageNumber, content, imageUrl);
                } catch (Exception e) {
                    return new SceneResult(finalPageNumber, content, null);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        futures.stream().map(CompletableFuture::join).sorted((a, b) -> Integer.compare(a.pageNumber, b.pageNumber))
                .forEach(sceneResult -> {
                    SummaryScene scene = new SummaryScene();
                    scene.setBookSummary(bookSummary);
                    scene.setPageNumber(sceneResult.pageNumber);
                    scene.setContent(sceneResult.content);
                    scene.setIllustrationUrl(sceneResult.illustrationUrl);
                    summarySceneRepository.save(scene);
                });
    }

    public List<SummarySceneResponse> getSummaryScenes(Long bookId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));
        return summarySceneRepository.findByBookSummary(summary).stream()
                .map(scene -> new SummarySceneResponse(
                        scene.getPageNumber(),
                        scene.getContent(),
                        scene.getIllustrationUrl()))
                .toList();
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private String loadPromptTemplate(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("프롬프트 템플릿을 불러오는 중 오류 발생", e);
        }
    }

    private static class SceneResult {
        int pageNumber;
        String content;
        String illustrationUrl;

        SceneResult(int pageNumber, String content, String illustrationUrl) {
            this.pageNumber = pageNumber;
            this.content = content;
            this.illustrationUrl = illustrationUrl;
        }
    }
}
