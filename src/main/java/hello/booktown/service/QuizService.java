package hello.booktown.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import hello.booktown.domain.*;
import hello.booktown.domain.enums.*;
import hello.booktown.dto.QuizSubmissionDto;
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
    private final QuizSubmissionGroupRepository quizSubmissionGroupRepository;

    public QuizService(SummarySceneRepository sceneRepository,
                       BookSummaryRepository bookSummaryRepository,
                       QuizRepository quizRepository,
                       QuizOptionRepository quizOptionRepository,
                       QuizSubmissionRepository submissionRepository,
                       UserRepository userRepository,
                       ChatClient.Builder chatClientBuilder, QuizSubmissionGroupRepository quizSubmissionGroupRepository) {
        this.sceneRepository = sceneRepository;
        this.bookSummaryRepository = bookSummaryRepository;
        this.quizRepository = quizRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.chatClient = chatClientBuilder.build();
        this.quizSubmissionGroupRepository = quizSubmissionGroupRepository;
    }

    @Value("classpath:/prompts/quiz-prompt.st")
    private Resource quizPromptResource;

    @Transactional
    public List<Quiz> createQuizzes(Long bookId, QuestionType type, Difficulty difficulty, Long userId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new RuntimeException("요약된 책 정보를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        List<Quiz> existing = quizRepository.findByBookSummaryAndQuestionTypeAndUser(summary, type, user);
        if (!existing.isEmpty()) {
            attachOptionsAndStripSummary(existing);
            return existing;
        }

        List<SummaryScene> scenes = sceneRepository.findByBookSummary(summary);
        if (scenes.isEmpty()) {
            throw new RuntimeException("요약된 씬 정보가 없습니다.");
        }

        // 🎯 무조건 10개 퀴즈 생성: 씬이 부족하면 반복 사용
        List<SummaryScene> selectedScenes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            selectedScenes.add(scenes.get(i % scenes.size())); // 순환
        }

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
        quiz.setScore(dto.getScore());
        quiz.setExplanation(dto.getExplanation());
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
        quiz.setScore(dto.getScore());
        quiz.setExplanation(dto.getExplanation());
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
        quiz.setScore(dto.getScore());
        quiz.setExplanation(dto.getExplanation());
        return quiz;
    }

    @Transactional
    public List<Boolean> submitBatch(Long userId, List<QuizSubmissionDto> submissions) {
        User user = userRepository.findById(userId).orElseThrow();
        Quiz quiz = quizRepository.findById(submissions.get(0).getQuizId()).orElseThrow();
        Book book = quiz.getBookSummary().getBook();

        int groupIndex = quizSubmissionGroupRepository.countByUserAndBook(user, book);

        QuizSubmissionGroup group = new QuizSubmissionGroup();
        group.setUser(user);
        group.setBook(book);
        group.setGroupIndex(groupIndex);
        quizSubmissionGroupRepository.save(group);

        List<Boolean> results = new ArrayList<>();

        for (QuizSubmissionDto dto : submissions) {
            Quiz q = quizRepository.findById(dto.getQuizId()).orElseThrow();
            boolean isCorrect = q.getCorrectAnswer().equalsIgnoreCase(dto.getAnswer());

            QuizSubmission submission = new QuizSubmission();
            submission.setQuiz(q);
            submission.setUserAnswer(dto.getAnswer());
            submission.setCorrect(isCorrect);
            submission.setSubmissionGroup(group);
            submissionRepository.save(submission);

            if (isCorrect) {
                user.updateScore(user.getScore() + q.getScore());
            }

            results.add(isCorrect);
        }

        userRepository.save(user);
        return results;
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

}
