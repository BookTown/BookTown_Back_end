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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SummaryService {

    private final BookRepository bookRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;
    private final RestTemplate restTemplate;
    private final ChatClient chatClient;

    @Value("classpath:/prompts/summary-prompt.st")
    private Resource summaryPrompt;

    public SummaryService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder,
                          BookRepository bookRepository, BookSummaryRepository bookSummaryRepository,
                          SummarySceneRepository summarySceneRepository) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.summarySceneRepository = summarySceneRepository;
    }

    public void summarizeBook(Long bookId) throws IOException {
        if (bookSummaryRepository.existsByBookId(bookId)) {
            return;
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        String bookText = fetchBookText(book.getSummaryUrl());

        List<String> chunkSummaries = summarizeChunksAsync(bookText);

        String fullSummary = String.join("\n\n", chunkSummaries);

        List<String> sceneSummaries = summarizeInto10Scenes(fullSummary);

        saveScenesParallel(book, sceneSummaries);
    }

    private List<String> summarizeChunksAsync(String bookText) {
        int chunkSize = 8000;
        List<String> chunks = splitTextIntoChunks(bookText, chunkSize);

        int poolSize = Math.min(chunks.size(), 10);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String chunk : chunks) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                String prompt = "다음 내용을 요약해줘:\n\n" + chunk;
                return chatClient.prompt(prompt).call().content().trim();
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> summaries = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();
        return summaries;
    }

    private List<String> summarizeInto10Scenes(String fullSummary) {
        String template = loadPromptTemplate();
        String prompt = template.replace("{{fullSummary}}", fullSummary);

        String result = chatClient.prompt(prompt).call().content().trim();
        List<String> scenes = List.of(result.split("\\n\\n"));

        return adjustScenesToTen(scenes);
    }

    private List<String> adjustScenesToTen(List<String> scenes) {
        List<String> validScenes = scenes.stream()
                .filter(scene -> scene.length() > 100)
                .toList();

        if (validScenes.size() > 10) {
            return validScenes.subList(0, 10);
        } else if (validScenes.size() < 10) {
            List<String> padded = new ArrayList<>(validScenes);
            while (padded.size() < 10) {
                padded.add("");
            }
            return padded;
        } else {
            return validScenes;
        }
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

            CompletableFuture<SceneResult> future = CompletableFuture.supplyAsync(() -> {
                return new SceneResult(pageNumber, content, "");
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SceneResult> sortedScenes = futures.stream()
                .map(CompletableFuture::join)
                .sorted((a, b) -> Integer.compare(a.pageNumber, b.pageNumber))
                .toList();

        for (SceneResult sceneResult : sortedScenes) {
            SummaryScene scene = new SummaryScene();
            scene.setBookSummary(bookSummary);
            scene.setPageNumber(sceneResult.pageNumber);
            scene.setContent(sceneResult.content);
            scene.setIllustrationUrl(null);

            summarySceneRepository.save(scene);
        }

        executor.shutdown();
    }

    private String fetchBookText(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .timeout(60000)
                .followRedirects(true).get();
        return doc.text();
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private String loadPromptTemplate() {
        try {
            return new String(summaryPrompt.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("프롬프트 템플릿을 불러오는 중 오류 발생", e);
        }
    }

    public List<SummarySceneResponse> getSummaryScenes(Long bookId) {
        BookSummary bookSummary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));

        return summarySceneRepository.findByBookSummary(bookSummary).stream()
                .map(scene -> new SummarySceneResponse(
                        scene.getPageNumber(),
                        scene.getContent(),
                        scene.getIllustrationUrl()
                ))
                .toList();
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
