import { ATTEMPT_STORAGE_PREFIX } from "../constants/attemptConstants";

const getExamSessionKey = (examId) => `${ATTEMPT_STORAGE_PREFIX}${examId}`;

export const saveExamSession = (examId, attemptId) => {
  if (!examId || !attemptId) return;
  try {
    const payload = JSON.stringify({
      examId: parseInt(examId, 10),
      attemptId: parseInt(attemptId, 10),
    });
    sessionStorage.setItem(getExamSessionKey(examId), payload);
  } catch {
    // Best effort: the server is the source of truth
  }
};
