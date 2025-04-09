//package hello.booktown.service;
//
//import hello.booktown.domain.Book;
//import hello.booktown.dto.GutendexResponse;
//import hello.booktown.repository.BookRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class GutendexService {
//
//    private final RestTemplate restTemplate;
//    private final BookRepository bookRepository;
//
////    public void fetchAndSaveBooks(String keyword) {
////        String url = "https://gutendex.com/books?search=" + keyword;
////
////        GutendexResponse response = restTemplate.getForObject(url, GutendexResponse.class);
////
////        if (response != null && response.getResults() != null) {
////            for (GutendexResponse.GutenbergBook gBook : response.getResults()) {
////                Book book = new Book();
////                book.setTitle(gBook.getTitle());
////
////                if (gBook.getAuthors() != null && !gBook.getAuthors().isEmpty()) {
////                    book.setAuthor(gBook.getAuthors().get(0).getName());
////                }
////
////                // ISBN과 출판연도는 구텐베르크에서 제공하지 않기 때문에 null 처리
////                book.setISBN(null);
////                book.setPublicationYear(null);
////
////                // ✅ plain text 형식의 요약용 URL 저장
////                if (gBook.getFormats() != null && gBook.getFormats().getPlainText() != null) {
////                    book.setSummaryUrl(gBook.getFormats().getPlainText());
////                }
////
////                bookRepository.save(book);
////            }
////        }
////    }
////}
