package hello.booktown.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hello.booktown.domain.Book;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.SummaryScene;
import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.BookSummaryRepository;
import hello.booktown.repository.SummarySceneRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final BookRepository bookRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;
    private final RestTemplate restTemplate;
    private final ChatClient chatClient;
    private final StabilityAIService stabilityAIService;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("classpath:/prompts/scene-outline-prompt.st")
    private Resource sceneOutlinePrompt;

    @Value("classpath:/prompts/scene-expansion-prompt.st")
    private Resource sceneExpansionPrompt;

    public SummaryService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder,
                          BookRepository bookRepository, BookSummaryRepository bookSummaryRepository,
                          SummarySceneRepository summarySceneRepository, StabilityAIService stabilityAIService,
                          TtsService ttsService) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.summarySceneRepository = summarySceneRepository;
        this.stabilityAIService = stabilityAIService;
        this.ttsService = ttsService;
    }

    public List<SummarySceneResponse> summarizeBook(Long bookId) throws IOException {
        if (bookSummaryRepository.existsByBookId(bookId)) {
            return getSummaryScenes(bookId);
        }

        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        String bookText = cleanGutenbergText(fetchBookText(book.getSummaryUrl()));

        List<String> chunkSummaries = summarizeChunksAsync(bookText);
        String fullSummary = String.join("\n\n", chunkSummaries);

        List<String> sceneOutlines = generateSceneOutline(fullSummary);
        List<String> sceneSummaries = expandScenes(sceneOutlines);

        saveScenesParallel(book, sceneSummaries);
        return getSummaryScenes(bookId);
    }

    private String fetchBookText(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(60000).followRedirects(true).get();
        return doc.text();
    }

    private String cleanGutenbergText(String rawText) {
        int startIndex = rawText.indexOf("*** START OF THE PROJECT GUTENBERG EBOOK");
        if (startIndex != -1) rawText = rawText.substring(startIndex + 40);
        int endIndex = rawText.indexOf("*** END OF THE PROJECT GUTENBERG EBOOK");
        if (endIndex != -1) rawText = rawText.substring(0, endIndex);
        return rawText.trim();
    }

    private List<String> summarizeChunksAsync(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 6000) {
            int end = Math.min(text.length(), i + 6000);
            chunks.add(text.substring(i, end));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunks.size(), 10));
        List<CompletableFuture<String>> futures = chunks.stream().map(chunk ->
                CompletableFuture.supplyAsync(() ->
                        chatClient.prompt("다음 내용을 간결하게 요약해줘: " + chunk).call().content().trim(), executor)
        ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private List<String> generateSceneOutline(String fullSummary) throws IOException {
        String prompt = loadPromptTemplate(sceneOutlinePrompt).replace("{{fullSummary}}", fullSummary);
        String result = chatClient.prompt(prompt).call().content().trim().replaceAll("```json|```", "");
        return objectMapper.readValue(result, new TypeReference<>() {});
    }

    private List<String> expandScenes(List<String> outlines) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<String>> futures = outlines.stream().map(outline ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String prompt = loadPromptTemplate(sceneExpansionPrompt).replace("{{sceneOutline}}", outline);
                        return chatClient.prompt(prompt).call().content().trim();
                    } catch (Exception e) {
                        return "";
                    }
                }, executor)
        ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private void saveScenesParallel(Book book, List<String> sceneSummaries) {
        BookSummary bookSummary = new BookSummary();
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < sceneSummaries.size(); i++) {
            int page = i + 1;
            String content = sceneSummaries.get(i);

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    String imageUrl = stabilityAIService.generateSceneImage(content, book.getId(), page);
                    String femaleAudioUrl = ttsService.generateAndUploadTts(content, book.getId(), page, SsmlVoiceGender.FEMALE);
                    String maleAudioUrl = ttsService.generateAndUploadTts(content, book.getId(), page, SsmlVoiceGender.MALE);

                    SummaryScene scene = new SummaryScene();
                    scene.setBookSummary(bookSummary);
                    scene.setPageNumber(page);
                    scene.setContent(content);
                    scene.setIllustrationUrl(imageUrl);
                    scene.setFemaleAudioUrl(femaleAudioUrl);
                    scene.setMaleAudioUrl(maleAudioUrl);
                    summarySceneRepository.save(scene);
                } catch (Exception e) {
                    log.error("장면 저장 중 오류 (page {}): {}", page, e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    private String loadPromptTemplate(Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public List<SummarySceneResponse> getSummaryScenes(Long bookId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));

        return summarySceneRepository.findByBookSummary(summary).stream()
                .map(scene -> new SummarySceneResponse(
                        scene.getPageNumber(),
                        scene.getContent(),
                        scene.getIllustrationUrl(),
                        scene.getFemaleAudioUrl(),
                        scene.getMaleAudioUrl()
                )).toList();
    }
}