import { useState, useCallback, useRef, useEffect, useMemo } from "react";
import { saveAttemptAnswer as apiSaveAttemptAnswer } from "../api/attemptApi";
import { getOptionAnswerId } from "../utils/examUtils";

export function useAttemptAnswers({
  examId,
  attemptId,
  questions,
  isAttemptExpired,
  showError,
  isMcqExam = true,
}) {
  const [mcqAnswers, setMcqAnswers] = useState({});
  const [textAnswers, setTextAnswers] = useState({});
  const answerSaveTimersRef = useRef({});

  useEffect(() => {
    const timers = answerSaveTimersRef.current;
    return () => {
      Object.values(timers).forEach((timer) => clearTimeout(timer));
    };
  }, []);

  const saveAnswerToBackend = useCallback(
    async (questionId, payload) => {
      if (!attemptId) {
        if (showError) {
          showError("Exam attempt is not ready. Reload the page and try again.");
        }
        return;
      }

      try {
        await apiSaveAttemptAnswer(examId, attemptId, questionId, payload);
      } catch (err) {
        if (showError) {
          showError(err.message || "Failed to save answer");
        }
      }
    },
    [attemptId, examId, showError]
  );

  const restoreSavedAnswers = useCallback(
    (savedAnswers, questionList = questions) => {
      if (!Array.isArray(savedAnswers) || savedAnswers.length === 0) return;

      setMcqAnswers((previous) => {
        const restored = { ...previous };
        savedAnswers.forEach((answer) => {
          const questionId = answer.questionId || answer.question_id;
          if (!questionId || answer.selectedAnswerId == null) {
            if (questionId) delete restored[questionId];
            return;
          }

          const question = questionList.find(
            (item) => String(item.question_id || item.questionId) === String(questionId)
          );
          if (!question) return;

          const options = ["option1", "option2", "option3", "option4"];
          const selectedOption = options.find((optionKey) => {
            const answerId = getOptionAnswerId(question, optionKey);
            return answerId != null && String(answerId) === String(answer.selectedAnswerId);
          });
          if (selectedOption && question[selectedOption]) {
            restored[questionId] = question[selectedOption];
          }
        });
        return restored;
      });

      setTextAnswers((previous) => {
        const restored = { ...previous };
        savedAnswers.forEach((answer) => {
          const questionId = answer.questionId || answer.question_id;
          if (!questionId) return;
          if (answer.textAnswer == null || answer.textAnswer === "") {
            delete restored[questionId];
          } else {
            restored[questionId] = answer.textAnswer;
          }
        });
        return restored;
      });
    },
    [questions]
  );

  const handleSelectOption = useCallback(
    (question, optionKey, optionText) => {
      if (isAttemptExpired) return;

      const questionId = question?.question_id || question?.questionId;
      if (!questionId) return;

      setMcqAnswers((prev) => ({
        ...prev,
        [questionId]: optionText,
      }));

      const selectedAnswerId = getOptionAnswerId(question, optionKey);
      if (!selectedAnswerId) {
        if (showError) {
          showError("Failed to save answer.");
        }
        return;
      }

      saveAnswerToBackend(questionId, { selectedAnswerId });
    },
    [isAttemptExpired, saveAnswerToBackend, showError]
  );

  const queueTextAnswerSave = useCallback(
    (questionId, value) => {
      const timerKey = String(questionId);
      if (answerSaveTimersRef.current[timerKey]) {
        clearTimeout(answerSaveTimersRef.current[timerKey]);
      }

      if (value.trim() === "") {
        delete answerSaveTimersRef.current[timerKey];
        saveAnswerToBackend(questionId, {});
        return;
      }

      answerSaveTimersRef.current[timerKey] = setTimeout(() => {
        delete answerSaveTimersRef.current[timerKey];
        saveAnswerToBackend(questionId, { textAnswer: value });
      }, 500);
    },
    [saveAnswerToBackend]
  );

  const handleTextAnswerChange = useCallback(
    (questionId, val) => {
      if (isAttemptExpired) return;

      setTextAnswers((prev) => ({
        ...prev,
        [questionId]: val,
      }));
      queueTextAnswerSave(questionId, val);
    },
    [isAttemptExpired, queueTextAnswerSave]
  );

  const answeredCount = useMemo(() => {
    if (isMcqExam) {
      return Object.keys(mcqAnswers).filter(
        (k) => mcqAnswers[k] && mcqAnswers[k].trim() !== ""
      ).length;
    }
    return Object.keys(textAnswers).filter(
      (k) => textAnswers[k] && textAnswers[k].trim() !== ""
    ).length;
  }, [isMcqExam, mcqAnswers, textAnswers]);

  return {
    mcqAnswers,
    textAnswers,
    setMcqAnswers,
    setTextAnswers,
    restoreSavedAnswers,
    handleSelectOption,
    handleTextAnswerChange,
    answeredCount,
  };
}
