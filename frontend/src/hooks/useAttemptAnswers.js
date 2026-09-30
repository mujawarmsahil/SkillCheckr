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
  // Latest value per question that has not been handed to the save queue yet. This is what
  // flushPendingSaves() sends, and it is the only place a pending value lives, so a value can
  // never be sent twice or dropped. The original question id is kept next to the value because the
  // map key is a string and the API must receive the id in its original type.
  const pendingTextRef = useRef({});
  // One promise per question, chained so two saves for the same question are never in flight at
  // the same time. An earlier, slower save landing after a later one would restore stale text.
  const inFlightRef = useRef({});
  // Set once submission has started. After that no further save may be sent, because the backend
  // rejects answers for a submitted attempt.
  const savesSuspendedRef = useRef(false);

  useEffect(() => {
    const timers = answerSaveTimersRef.current;
    return () => {
      Object.values(timers).forEach((timer) => clearTimeout(timer));
    };
  }, []);

  const saveAnswerToBackend = useCallback(
    async (questionId, payload) => {
      if (savesSuspendedRef.current) {
        return false;
      }
      if (!attemptId) {
        if (showError) {
          showError("Exam attempt is not ready. Reload the page and try again.");
        }
        return false;
      }

      try {
        await apiSaveAttemptAnswer(examId, attemptId, questionId, payload);
        return true;
      } catch (err) {
        if (showError) {
          showError(err.message || "Failed to save answer");
        }
        return false;
      }
    },
    [attemptId, examId, showError]
  );

  /**
   * Appends a save behind any save already running for the same question and returns a promise
   * that resolves to true when the answer was persisted. A failure of an earlier save does not
   * block the queue, it is only reported through the promise this call hands back.
   */
  const enqueueSave = useCallback(
    (questionId, payload) => {
      const previous = inFlightRef.current[questionId] || Promise.resolve();
      const next = previous
        .catch(() => false)
        .then(() => saveAnswerToBackend(questionId, payload))
        .finally(() => {
          if (inFlightRef.current[questionId] === next) {
            delete inFlightRef.current[questionId];
          }
        });
      inFlightRef.current[questionId] = next;
      return next;
    },
    [saveAnswerToBackend]
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

      // MCQ keeps its immediate save, unchanged, but it goes through the same per-question queue
      // so a flush can wait for it.
      enqueueSave(questionId, { selectedAnswerId });
    },
    [enqueueSave, isAttemptExpired, showError]
  );

  const queueTextAnswerSave = useCallback(
    (questionId, value) => {
      const timerKey = String(questionId);
      if (answerSaveTimersRef.current[timerKey]) {
        clearTimeout(answerSaveTimersRef.current[timerKey]);
        delete answerSaveTimersRef.current[timerKey];
      }

      if (value.trim() === "") {
        // Clearing was already an immediate save before this change and stays one, so there is
        // nothing left pending for this question.
        delete pendingTextRef.current[timerKey];
        enqueueSave(questionId, {});
        return;
      }

      pendingTextRef.current[timerKey] = { questionId, value };
      answerSaveTimersRef.current[timerKey] = setTimeout(() => {
        delete answerSaveTimersRef.current[timerKey];
        const queued = pendingTextRef.current[timerKey];
        if (queued === undefined) return;
        delete pendingTextRef.current[timerKey];
        enqueueSave(queued.questionId, { textAnswer: queued.value });
      }, 500);
    },
    [enqueueSave]
  );

  const handleTextAnswerChange = useCallback(
    (questionId, val) => {
      if (isAttemptExpired || savesSuspendedRef.current) return;

      setTextAnswers((prev) => ({
        ...prev,
        [questionId]: val,
      }));
      queueTextAnswerSave(questionId, val);
    },
    [isAttemptExpired, queueTextAnswerSave]
  );

  /**
   * Sends every text answer that is still sitting behind the debounce and waits until all saves
   * are finished. Callers must not submit the exam until this resolves, otherwise the submission
   * reaches the server before the answer and the server rejects the late save.
   *
   * @returns true when every answer reached the server, false when at least one save failed
   */
  const flushPendingSaves = useCallback(async () => {
    const timers = answerSaveTimersRef.current;
    Object.keys(timers).forEach((key) => {
      clearTimeout(timers[key]);
    });
    answerSaveTimersRef.current = {};

    const pending = pendingTextRef.current;
    pendingTextRef.current = {};

    const flushed = Object.values(pending).map((entry) =>
      enqueueSave(entry.questionId, { textAnswer: entry.value })
    );

    // Saves that were already running when the flush started have to be awaited too, otherwise
    // the exam could be submitted while one of them is still in flight.
    const running = Object.values(inFlightRef.current);

    const results = await Promise.all([...flushed, ...running]);
    return results.every(Boolean);
  }, [enqueueSave]);

  const suspendSaves = useCallback(() => {
    savesSuspendedRef.current = true;
  }, []);

  /**
   * Re-enables saving after a failed submission. The attempt is still open in that case, so the
   * student has to be able to correct the answer and try again.
   */
  const resumeSaves = useCallback(() => {
    savesSuspendedRef.current = false;
  }, []);

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
    restoreSavedAnswers,
    handleSelectOption,
    handleTextAnswerChange,
    answeredCount,
    flushPendingSaves,
    suspendSaves,
    resumeSaves,
  };
}
