package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.Scene;
import hello.booktown.dto.BookResponse;
import hello.booktown.dto.GutendexResponse;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.SceneRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private final RestTemplate restTemplate;
    private final ChatClient chatClient; // GPT 요약용 커스텀 서비스
    private final BookRepository bookRepository;
    private final SceneRepository sceneRepository;
    private final StabilityAIService stabilityAIService;

    public BookService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder, BookRepository bookRepository, SceneRepository sceneRepository, StabilityAIService stabilityAIService) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
        this.sceneRepository = sceneRepository;
        this.stabilityAIService = stabilityAIService;
    }

    //구텐베르크 책 ID를 통해 BookDB에 책 정보를 저장함
    public Book saveBookFromGutenberg(Long gutenbergId) {
        String metadataUrl = "https://gutendex.com/books/" + gutenbergId;
        ResponseEntity<GutendexResponse> response = restTemplate.getForEntity(metadataUrl, GutendexResponse.class);
        GutendexResponse bookData = response.getBody();

        if (bookData == null) {
            throw new RuntimeException("Gutenberg 책 정보를 가져올 수 없습니다.");
        }

        String originalTitle = bookData.getTitle();
        String originalAuthor = bookData.getAuthors() != null && !bookData.getAuthors().isEmpty()
                ? bookData.getAuthors().get(0).getName()
                : null;

        // 1. GPT 번역 (책 제목 먼저 확보)
        String translatedTitle = chatClient.prompt("다음 영어 책 제목을 한국어로 번역해줘 책 이름만 반환해 한국어 번역판의 이름 한 종류만 단답식으로 반환해: " + originalTitle)
                .call()
                .content()
                .trim();

        String translatedAuthor = originalAuthor != null
                ? chatClient.prompt("다음 영어 작가 이름을 한국어로 번역해줘 작가 이름만 반환해: " + originalAuthor)
                .call()
                .content()
                .trim()
                : null;

        // 2. 이미지 생성 프롬프트 준비
        String imagePrompt = "Create a cute anime-style book cover with soft colors. Title: " + originalTitle;

        String textUrl = bookData.getFormats().entrySet().stream()
                .filter(e -> e.getKey().contains("text/plain") && !e.getKey().contains(".zip"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("텍스트 URL을 찾을 수 없습니다."));

        // 3. 썸네일 생성 및 업로드 (여기서 translatedTitle 넘김)
        String thumbnailUrl = stabilityAIService.generateThumbnail(imagePrompt, originalTitle); // 여기를 수정

        // 4. Book 저장
        Book book = new Book();
        book.setTitle(translatedTitle);
        book.setAuthor(translatedAuthor);
        book.setSummaryUrl(textUrl);
        book.setThumbnailUrl(thumbnailUrl);

        return bookRepository.save(book);
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

    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        return BookResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .summaryUrl(book.getSummaryUrl())
                .thumbnailUrl(book.getThumbnailUrl())
                .build();
    }

    public Book getRandomBook() {
        return bookRepository.findRandomBook().orElseThrow(() -> new RuntimeException("책이 없습니다"));
    }

    public List<Book> getLatestBooks() {
        return bookRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<Book> getTopLikedBooks() {
        return bookRepository.findTop10ByOrderByLikesDesc();
    }

    public List<Book> getAllBooksByLikes() {
        return bookRepository.findAllByOrderByLikesDesc();
    }

    public List<Book> getAllBooksByCreatedAt() {
        return bookRepository.findAllByOrderByCreatedAtDesc();
    }


}
