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

    @Value("classpath:/prompts/summary-prompt.st")
    private Resource summaryPrompt;

    @Value("classpath:/prompts/diffusion-bulk-prompt.st")
    private Resource diffusionPrompt;

    @Value("classpath:/prompts/character-dictionary-prompt.st")
    private Resource characterDictionaryPrompt;

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

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        String bookText = fetchBookText(book.getSummaryUrl());
        bookText = cleanGutenbergText(bookText);

        List<String> chunkSummaries = summarizeChunksAsync(bookText);
        String fullSummary = String.join("\n\n", chunkSummaries);
        log.info("summarizeChunksAsync 결과 총 요약 문자 수: {}", fullSummary.length());

        List<String> sceneSummaries = summarizeInto10Scenes(fullSummary);

        String characterDictionary = callCharacterExtractionService(book.getTitle(), sceneSummaries);

        saveScenesParallel(book, sceneSummaries, characterDictionary);
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
        List<String> chunks = splitTextIntoChunks(text, 8000);
        log.info("splitTextIntoChunks size: {}", chunks.size());
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunks.size(), 10));
        List<CompletableFuture<String>> futures = new ArrayList<>();

        int index = 1;
        for (String chunk : chunks) {
            int currentIndex = index++;
            futures.add(CompletableFuture.supplyAsync(() -> {
                String prompt = "다음 내용을 최대한 짧고 간결하게 요약하되, 주요 인물들의 사건은 포함시켜. 불필요한 문장은 무조건 최대한 제거해. 내용: " + chunk;
                String result = chatClient.prompt(prompt).call().content().trim();
                log.info("청크 {} 원본: {}자 → 요약: {}자", currentIndex, chunk.length(), result.length());
                return result;
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private List<String> summarizeInto10Scenes(String fullSummary) throws IOException {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String prompt = loadPromptTemplate(summaryPrompt).replace("{{fullSummary}}", fullSummary);
            String result = chatClient.prompt(prompt).call().content().trim().replaceAll("```json|```", "");

            List<String> scenes;
            try {
                scenes = objectMapper.readValue(result, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("summarizeInto10Scenes JSON 파싱 오류 (시도 {}): {}", attempt, e.getMessage());
                continue; // 파싱 오류 시 재시도
            }

            if (scenes.size() == 10) {
                return scenes;
            } else {
                log.warn("summarizeInto10Scenes 결과 {}개 → 10개가 될 때까지 재요청 시도 중 (현재 시도 {}회)", scenes.size(), attempt);
            }
        }

        // 그래도 안 되면 마지막 결과에 placeholder 추가해서 강제 10개로 맞춤
        log.warn("summarizeInto10Scenes 3회 시도 후에도 10개 미만 → placeholder 추가로 강제 보정");
        String placeholder = "빈 장면입니다. 이 부분은 추가 작성이 필요합니다.";
        List<String> fallbackScenes = new ArrayList<>();
        try {
            String prompt = loadPromptTemplate(summaryPrompt).replace("{{fullSummary}}", fullSummary);
            String result = chatClient.prompt(prompt).call().content().trim().replaceAll("```json|```", "");
            fallbackScenes = objectMapper.readValue(result, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Fallback summarizeInto10Scenes JSON 파싱 오류 → 빈 리스트 사용");
        }

        while (fallbackScenes.size() < 10) {
            fallbackScenes.add(placeholder);
        }

        return fallbackScenes;
    }

    private String callCharacterExtractionService(String bookTitle, List<String> scenes) throws IOException {
        String scenesJson = objectMapper.writeValueAsString(scenes);
        String prompt = loadPromptTemplate(characterDictionaryPrompt)
                .replace("{{bookTitle}}", bookTitle)
                .replace("{{sceneListJson}}", scenesJson);
        String result = chatClient.prompt(prompt).call().content().trim();

        log.info("캐릭터 딕셔너리 추출 결과:\n{}", result);

        return result;
    }

    private void saveScenesParallel(Book book, List<String> sceneSummaries, String characterDictionary) throws IOException {
        BookSummary bookSummary = new BookSummary();
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        List<String> firstSentences = sceneSummaries.stream()
                .map(content -> content.split("[.?!]")[0].trim())
                .toList();

        String jsonArray = objectMapper.writeValueAsString(firstSentences);
        log.info("diffusion bulk 프롬프트에 전달된 firstSentences JSON:\n{}", jsonArray);
        String diffusionBulkPromptText = loadPromptTemplate(diffusionPrompt)
                .replace("{{sceneListJson}}", jsonArray)
                .replace("{{bookTitle}}", book.getTitle())
                .replace("{{characterAppearanceDictionary}}", characterDictionary);

        String bulkResult = chatClient.prompt(diffusionBulkPromptText).call().content().trim();
        bulkResult = bulkResult.replaceAll("```json|```", "").trim();
        List<String> diffusionPrompts = objectMapper.readValue(bulkResult, new TypeReference<>() {});
        log.info("GPT diffusion 프롬프트 10개 JSON 응답:\n{}", diffusionPrompts);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<SceneResult>> futures = new ArrayList<>();

        for (int i = 0; i < Math.min(sceneSummaries.size(), diffusionPrompts.size()); i++) {
            int pageNumber = i + 1;
            String content = sceneSummaries.get(i).trim();
            String generatedPrompt = diffusionPrompts.get(i).trim();
            if (content.isEmpty() || generatedPrompt.isEmpty()) continue;

            int finalPageNumber = pageNumber;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    String imageUrl = stabilityAIService.generateSceneImage(generatedPrompt, book.getId(), finalPageNumber);
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
            // Store both female and male audio URLs
            scene.setFemaleAudioUrl(sceneResult.femaleAudioUrl);
            scene.setMaleAudioUrl(sceneResult.maleAudioUrl);
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
                        scene.getIllustrationUrl(),
                        scene.getFemaleAudioUrl(),
                        scene.getMaleAudioUrl()
                ))
                .toList();
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(text.length(), i + chunkSize);
            chunks.add(text.substring(i, end));

        }
        return chunks;
    }

    private String loadPromptTemplate(Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
