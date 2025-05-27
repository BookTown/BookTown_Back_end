package hello.booktown.service;

import com.google.cloud.texttospeech.v1.*;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import java.io.InputStream;
import java.io.FileInputStream;
import com.google.protobuf.ByteString;
import hello.booktown.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TtsService {

    private final S3Uploader s3Uploader;

    @Value("${google.credentials.location}")
    private String credentialsLocation;

    public String generateAndUploadTts(String text, Long bookId, int pageNumber, SsmlVoiceGender gender) {
        try (InputStream credentialsStream = new FileInputStream(credentialsLocation.replace("file:", ""))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
                // 1. TTS 요청 생성
                SynthesisInput input = SynthesisInput.newBuilder()
                        .setText(text)
                        .build();

                String voiceName = gender == SsmlVoiceGender.MALE ? "ko-KR-Standard-D" : "ko-KR-Standard-B";
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode("ko-KR")
                        .setName(voiceName)
                        .setSsmlGender(gender)
                        .build();

                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .build();

                // 2. TTS 응답 받기
                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                ByteString audioContents = response.getAudioContent();

                // 3. 임시 파일로 저장
                String fileName = "tts/book-" + bookId + "/scene-" + pageNumber + "-" + gender.toString().toLowerCase() + ".mp3";
                File tempFile = File.createTempFile("tts-", ".mp3");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    out.write(audioContents.toByteArray());
                }

                // 4. S3에 업로드
                String url = s3Uploader.upload(tempFile, fileName);

                // 5. 임시 파일 삭제
                tempFile.delete();

                return url;
            }
        } catch (IOException e) {
            throw new RuntimeException("TTS 생성 실패", e);
        }
    }
}