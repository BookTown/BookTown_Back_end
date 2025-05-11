package hello.booktown.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import hello.booktown.domain.*;
import hello.booktown.domain.enums.*;
import hello.booktown.dto.quizType.*;
import hello.booktown.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {
    private final SummarySceneRepository sceneRepository;
    private final BookSummaryRepository bookSummaryRepository;
    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(SummarySceneRepository sceneRepository,
                       BookSummaryRepository bookSummaryRepository,
                       QuizRepository quizRepository,
                       QuizOptionRepository quizOptionRepository,
                       QuizSubmissionRepository submissionRepository,
                       UserRepository userRepository,
                       ChatClient.Builder chatClientBuilder) {
        this.sceneRepository = sceneRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.quizRepository = quizRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.chatClient = chatClientBuilder.build();
    }

    @Value("classpath:/prompts/quiz-prompt.st")
    private Resource quizPromptResource;

    @Transactional
    public List<Quiz> createQuizzes(Long bookId, QuestionType type, Difficulty difficulty, Long userId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // ✅ 기존 퀴즈가 존재하면 그대로 반환
        List<Quiz> existing = quizRepository.findByBookSummaryAndQuestionTypeAndUser(summary, type, user);
        if (!existing.isEmpty()) {
            attachOptionsAndStripSummary(existing);
            return existing;
        }

        // ✅ 새로운 퀴즈 생성
        List<SummaryScene> scenes = sceneRepository.findByBookSummary(summary);
        if (scenes.size() < 10) {
            throw new IllegalStateException("씬이 부족하여 퀴즈를 생성할 수 없습니다. (10개 필요)");
        }

        List<SummaryScene> selectedScenes = scenes.subList(0, 10);
        for (SummaryScene scene : selectedScenes) {
            try {
                String prompt = buildQuizPrompt(scene.getContent(), type, difficulty);
                String response = chatClient.prompt(prompt).call().content();
                saveQuizByType(summary, user, scene, response, type);
            } catch (Exception e) {
                throw new RuntimeException("퀴즈 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }

        List<Quiz> created = quizRepository.findByBookSummaryAndQuestionTypeAndUser(summary, type, user);
        attachOptionsAndStripSummary(created);
        return created;
    }


    private void attachOptionsAndStripSummary(List<Quiz> quizzes) {
        for (Quiz quiz : quizzes) {
            BookSummary minimalSummary = new BookSummary();
            minimalSummary.setId(quiz.getBookSummary().getId());
            quiz.setBookSummary(minimalSummary);

            if (quiz.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                List<QuizOption> options = quizOptionRepository.findByQuiz(quiz);
                quiz.setOptions(options);
            } else {
                quiz.setOptions(null);
            }
        }
    }

    private void saveQuizByType(BookSummary summary, User user, SummaryScene scene, String json, QuestionType type) {
        try {
            switch (type) {
                case MULTIPLE_CHOICE -> saveMultipleChoiceQuiz(summary, user, json);
                case TRUE_FALSE -> saveTrueFalseQuiz(summary, user, json);
                case SHORT_ANSWER -> saveShortAnswerQuiz(summary, user, json);
            }
        } catch (Exception e) {
            throw new RuntimeException("퀴즈 저장 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private void saveMultipleChoiceQuiz(BookSummary summary, User user, String json) throws JsonProcessingException {
        MultipleChoiceQuizDto dto = objectMapper.readValue(json, MultipleChoiceQuizDto.class);

        Quiz quiz = toEntity(summary, user, dto);
        quizRepository.save(quiz);

        List<QuizOption> options = new ArrayList<>();
        for (int i = 0; i < dto.getOptions().size(); i++) {
            QuizOption option = new QuizOption();
            option.setQuiz(quiz);
            option.setIndex(i);
            option.setText(dto.getOptions().get(i));
            quizOptionRepository.save(option);
            options.add(option);
        }

        quiz.setOptions(options);
    }

    private void saveTrueFalseQuiz(BookSummary summary, User user, String json) throws JsonProcessingException {
        TrueFalseQuizDto dto = objectMapper.readValue(json, TrueFalseQuizDto.class);
        quizRepository.save(toEntity(summary, user, dto));
    }

    private void saveShortAnswerQuiz(BookSummary summary, User user, String json) throws JsonProcessingException {
        ShortAnswerQuizDto dto = objectMapper.readValue(json, ShortAnswerQuizDto.class);
        quizRepository.save(toEntity(summary, user, dto));
    }

    private Quiz toEntity(BookSummary summary, User user, MultipleChoiceQuizDto dto) {
        Quiz quiz = new Quiz();
        quiz.setBookSummary(summary);
        quiz.setUser(user);
        quiz.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        quiz.setDifficulty(Difficulty.valueOf(dto.getDifficulty()));
        quiz.setQuestion(dto.getQuestion());
        quiz.setCorrectAnswer(dto.getOptions().get(dto.getAnswerIndex()));
        quiz.setScore(10);
        return quiz;
    }


    private Quiz toEntity(BookSummary summary, User user, TrueFalseQuizDto dto) {
        Quiz quiz = new Quiz();
        quiz.setBookSummary(summary);
        quiz.setUser(user);
        quiz.setQuestionType(QuestionType.TRUE_FALSE);
        quiz.setDifficulty(Difficulty.valueOf(dto.getDifficulty()));
        quiz.setQuestion(dto.getQuestion());
        quiz.setCorrectAnswer(dto.getAnswer());
        quiz.setScore(5);
        return quiz;
    }


    private Quiz toEntity(BookSummary summary, User user, ShortAnswerQuizDto dto) {
        Quiz quiz = new Quiz();
        quiz.setBookSummary(summary);
        quiz.setUser(user);
        quiz.setQuestionType(QuestionType.SHORT_ANSWER);
        quiz.setDifficulty(Difficulty.valueOf(dto.getDifficulty().toUpperCase()));
        quiz.setQuestion(dto.getQuestion());
        quiz.setCorrectAnswer(dto.getAnswer());
        quiz.setScore(15);
        return quiz;
    }


    @Transactional
    public boolean submitAnswer(Long userId, Long quizId, String answer) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        boolean isCorrect = quiz.getCorrectAnswer().equalsIgnoreCase(answer);

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setUser(userRepository.findById(userId).orElseThrow());
        submission.setUserAnswer(answer);
        submission.setCorrect(isCorrect);
        submissionRepository.save(submission);

        if (isCorrect) {
            User user = submission.getUser();
            user.updateScore(user.getScore() + quiz.getScore());
            userRepository.save(user);
        }

        return isCorrect;
    }

    private String buildQuizPrompt(String content, QuestionType type, Difficulty difficulty) {
        try {
            String template = new String(quizPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template
                    .replace("{{type}}", type.name())
                    .replace("{{difficulty}}", difficulty.name())
                    .replace("{{content}}", content);
        } catch (IOException e) {
            throw new RuntimeException("퀴즈 프롬프트 읽기 실패", e);
        }
    }

    public List<QuizOption> getOptionsForQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        return quizOptionRepository.findByQuiz(quiz);
    }
}
