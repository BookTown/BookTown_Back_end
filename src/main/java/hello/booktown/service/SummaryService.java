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
import org.springframework.ai.chat.client.ChatClient;
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

    public void summarizeBookForUser(Long userId, Long bookId) throws IOException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 1. 책 텍스트 가져오기
        String bookText = fetchBookText(book.getSummaryUrl());

        // 2. (1차 요약) 비동기 병렬 청크 요약
        List<String> chunkSummaries = summarizeChunksAsync(bookText);

        // 3. 1차 요약 결과 합치기
        String fullSummary = String.join("\n\n", chunkSummaries);

        // 4. (2차 요약) 10개 씬으로 압축 요약 (씬당 500자 내외)
        List<String> sceneSummaries = summarizeInto10Scenes(fullSummary);

        // 5. 씬 저장 (병렬 처리)
        saveScenesParallel(user, book, sceneSummaries);
    }

    private List<String> summarizeChunksAsync(String bookText) {
        int chunkSize = 8000;
        List<String> chunks = splitTextIntoChunks(bookText, chunkSize);

        int poolSize = Math.min(chunks.size(), 10);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String chunk : chunks) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                String prompt = "Summarize the following book content in Korean, focusing only on the main storyline without introduction or conclusion:\n\n" + chunk;
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
        String prompt = "다음 줄거리를 10개의 장면(Scene)으로 나누어 요약해줘. 각 장면은 약 500자 분량으로 풍성하게 작성하고, 스토리 연결성을 유지해줘:\n\n" + fullSummary;
        String result = chatClient.prompt(prompt).call().content().trim();
        List<String> scenes = List.of(result.split("\\n\\n"));

        return adjustScenesToTen(scenes);
    }

    private List<String> adjustScenesToTen(List<String> scenes) {
        List<String> validScenes = scenes.stream()
                .filter(scene -> !scene.toLowerCase().contains("project gutenberg"))
                .filter(scene -> !scene.toLowerCase().contains("license"))
                .filter(scene -> !scene.toLowerCase().contains("terms"))
                .filter(scene -> scene.length() > 100)
                .toList();

        if (validScenes.size() > 10) {
            return validScenes.subList(0, 10);
        } else if (validScenes.size() < 10) {
            List<String> padded = new ArrayList<>(validScenes);
            while (padded.size() < 10) {
                padded.add(""); // 빈 씬 추가
            }
            return padded;
        } else {
            return validScenes;
        }
    }

    // ✅ saveScenes 병렬 버전
    private void saveScenesParallel(User user, Book book, List<String> sceneSummaries) {
        BookSummary bookSummary = new BookSummary();
        bookSummary.setUser(user);
        bookSummary.setBook(book);
        bookSummaryRepository.save(bookSummary);

        ExecutorService executor = Executors.newFixedThreadPool(5); // 동시에 5개 처리

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < sceneSummaries.size(); i++) {
            int pageNumber = i + 1;
            String content = sceneSummaries.get(i).trim();

            if (content.isEmpty()) continue; // 빈 씬 건너뜀

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 1. 한국어 -> 영어 번역
                    String translatedPrompt = chatClient.prompt(
                            "Translate the following Korean scene summary into fluent English. Return only the translated text, no explanations:\n\n" + content
                    ).call().content().trim();

                    // 2. StabilityAI로 이미지 생성
                    String imageUrl = stabilityAIService.generateSceneImage(translatedPrompt, user.getId(), book.getId(), pageNumber);

                    // 3. SummaryScene 저장
                    SummaryScene scene = new SummaryScene();
                    scene.setBookSummary(bookSummary);
                    scene.setPageNumber(pageNumber);
                    scene.setContent(content);
                    scene.setIllustrationUrl(imageUrl);

                    summarySceneRepository.save(scene);

                } catch (Exception e) {
                    e.printStackTrace();
                    // 실패해도 전체 프로세스는 중단하지 않음
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    private String fetchBookText(String url) throws IOException {
        Document doc = Jsoup.connect(url).followRedirects(true).get();
        return doc.text();
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
