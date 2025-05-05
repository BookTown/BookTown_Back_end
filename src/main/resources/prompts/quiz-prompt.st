다음 내용을 바탕으로 퀴즈를 만들어줘. 퀴즈 유형은 {{type}}이야.
아래 예시처럼 **정확한 JSON 객체 하나만** 반환해. 문자열 이외 아무것도 넣지 마.

예시들:

// 객관식 (MULTIPLE_CHOICE)
{
  "type": "MULTIPLE_CHOICE",
  "difficulty": "MEDIUM",
  "question": "문제?",
  "options": ["보기1", "보기2", "보기3", "보기4"],
  "answerIndex": 1,
  "score": 10
}

// OX (TRUE_FALSE)
{
  "type": "TRUE_FALSE",
  "difficulty": "EASY",
  "question": "문제?",
  "answer": "TRUE",
  "score": 5
}

// 주관식 (SHORT_ANSWER)
{
  "type": "SHORT_ANSWER",
  "difficulty": "HARD",
  "question": "문제?",
  "answer": "정답",
  "score": 15
}

내용: {{content}}
