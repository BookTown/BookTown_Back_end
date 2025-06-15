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

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final BookRepository bookRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StabilityAIService stabilityAIService;
    private final TtsService ttsService;

    @Value("classpath:/prompts/scene-outline-prompt.st")
    private Resource sceneOutlinePrompt;

    @Value("classpath:/prompts/scene-expansion-prompt.st")
    private Resource sceneExpansionPrompt;

    public SummaryService(ChatClient.Builder chatClientBuilder,
                          BookRepository bookRepository,
                          BookSummaryRepository bookSummaryRepository,
                          SummarySceneRepository summarySceneRepository,
                          StabilityAIService stabilityAIService,
                          TtsService ttsService) {
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

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        String bookText = fetchBookText(book.getSummaryUrl());
        bookText = cleanGutenbergText(bookText);

        List<String> chunkSummaries = summarizeChunksAsync(bookText);
        String fullSummary = String.join("\n\n", chunkSummaries);

        List<String> sceneOutlines = generateSceneOutline(fullSummary);
        List<String> sceneSummaries = expandScenes(sceneOutlines);

        BookSummary bookSummary = new BookSummary();
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<SceneResult>> futures = new ArrayList<>();

        for (int i = 0; i < sceneSummaries.size(); i++) {
            int pageNumber = i + 1;
            String content = sceneSummaries.get(i);
            int finalPageNumber = pageNumber;

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = content; // 이미지 생성 프롬프트로 content 활용
                    String imageUrl = stabilityAIService.generateSceneImage(prompt, book.getId(), finalPageNumber);
                    String femaleAudioUrl = ttsService.generateAndUploadTts(content, book.getId(), finalPageNumber, SsmlVoiceGender.FEMALE);
                    String maleAudioUrl = ttsService.generateAndUploadTts(content, book.getId(), finalPageNumber, SsmlVoiceGender.MALE);
                    return new SceneResult(finalPageNumber, content, imageUrl, femaleAudioUrl, maleAudioUrl);
                } catch (Exception e) {
                    return new SceneResult(finalPageNumber, content, null, null, null);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        futures.stream().map(CompletableFuture::join).sorted(Comparator.comparingInt(a -> a.pageNumber)).forEach(sceneResult -> {
            SummaryScene scene = new SummaryScene();
            scene.setBookSummary(bookSummary);
            scene.setPageNumber(sceneResult.pageNumber);
            scene.setContent(sceneResult.content);
            scene.setIllustrationUrl(sceneResult.illustrationUrl);
            scene.setFemaleAudioUrl(sceneResult.femaleAudioUrl);
            scene.setMaleAudioUrl(sceneResult.maleAudioUrl);
            summarySceneRepository.save(scene);
        });

        return getSummaryScenes(bookId);
    }

    private String fetchBookText(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(60000).followRedirects(true).get();
        return doc.text();
    }

    private String cleanGutenbergText(String rawText) {
        String text = rawText;
        int startIndex = text.indexOf("*** START OF THE PROJECT GUTENBERG EBOOK");
        if (startIndex != -1) text = text.substring(startIndex + 40);
        int endIndex = text.indexOf("*** END OF THE PROJECT GUTENBERG EBOOK");
        if (endIndex != -1) text = text.substring(0, endIndex);
        return text.trim();
    }

    private List<String> summarizeChunksAsync(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 6000) {
            int end = Math.min(text.length(), i + 6000);
            chunks.add(text.substring(i, end));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunks.size(), 10));
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String chunk : chunks) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                String prompt = "다음 내용을 최대한 간결하게 요약하되, 사건의 흐름이 드러나도록 해줘. 내용: " + chunk;
                return chatClient.prompt(prompt).call().content().trim();
            }, executor));
        }
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
        List<String> scenes = new ArrayList<>();
        for (String outline : outlines) {
            String prompt = loadPromptTemplate(sceneExpansionPrompt).replace("{{sceneOutline}}", outline);
            String result = chatClient.prompt(prompt).call().content().trim();
            scenes.add(result);
        }
        return scenes;
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
                ))
                .toList();
    }

    private static class SceneResult {
        int pageNumber;
        String content;
        String illustrationUrl;
        String femaleAudioUrl;
        String maleAudioUrl;

        SceneResult(int pageNumber, String content, String illustrationUrl, String femaleAudioUrl, String maleAudioUrl) {
            this.pageNumber = pageNumber;
            this.content = content;
            this.illustrationUrl = illustrationUrl;
            this.femaleAudioUrl = femaleAudioUrl;
            this.maleAudioUrl = maleAudioUrl;
        }
    }
}
