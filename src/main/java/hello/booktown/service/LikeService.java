package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookLike;
import hello.booktown.domain.User;
import hello.booktown.repository.BookLikeRepository;
import hello.booktown.repository.BookRepository;
import hello.booktown.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final BookRepository bookRepository;
    private final BookLikeRepository bookLikeRepository;
    private final UserRepository userRepository;

    public boolean toggleLike(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Optional<BookLike> existingLike = bookLikeRepository.findByBookAndUser(book, user);

        if (existingLike.isPresent()) {
            // 이미 좋아요를 눌렀으면 취소
            bookLikeRepository.delete(existingLike.get());
            book.setLikeCount(book.getLikeCount() - 1);
            bookRepository.save(book);
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            BookLike bookLike = new BookLike();
            bookLike.setBook(book);
            bookLike.setUser(user);
            bookLikeRepository.save(bookLike);
            book.setLikeCount(book.getLikeCount() + 1);
            bookRepository.save(book);
            return true; // 좋아요 추가됨
        }
    }

}
