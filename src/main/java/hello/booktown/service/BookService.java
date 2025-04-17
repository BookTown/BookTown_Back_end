package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.dto.BookResponse;
import hello.booktown.dto.GutendexResponse;
import hello.booktown.repository.BookRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private final RestTemplate restTemplate;
    private final ChatClient chatClient; // GPT 요약용 커스텀 서비스
    private final BookRepository bookRepository;
    private final StabilityAIService stabilityAIService;

    public BookService(RestTemplate restTemplate, ChatClient.Builder chatClientBuilder, BookRepository bookRepository,  StabilityAIService stabilityAIService) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClientBuilder.build();
        this.bookRepository = bookRepository;
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
        String imagePrompt = "Draw an illustration of the main character of the book with a background that depicts the overall atmosphere of the story." + originalTitle;

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

    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));

        return BookResponse.builder()
                .bookId(book.getId())
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
        return bookRepository.findTop10ByOrderByLikeCountDesc();
    }

    public List<Book> getAllBooksByLikes() {
        return bookRepository.findAllByOrderByLikeCountDesc();
    }

    public List<Book> getAllBooksByCreatedAt() {
        return bookRepository.findAllByOrderByCreatedAtDesc();
    }

    public BookResponse getBookInfo(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 책을 찾을 수 없습니다."));

        return BookResponse.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .summaryUrl(book.getSummaryUrl())
                .thumbnailUrl(book.getThumbnailUrl())
                .likecount(book.getLikeCount())
                .build();
    }


}
