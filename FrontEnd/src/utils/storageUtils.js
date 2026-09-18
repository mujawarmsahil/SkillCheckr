import { ATTEMPT_STORAGE_PREFIX } from "../constants/attemptConstants";

export const getExamSessionKey = (examId) => `${ATTEMPT_STORAGE_PREFIX}${examId}`;

export const saveExamSession = (examId, attemptId) => {
  if (!examId || !attemptId) return;
  try {
    const payload = JSON.stringify({
      examId: parseInt(examId, 10),
      attemptId: parseInt(attemptId, 10),
    });
    sessionStorage.setItem(getExamSessionKey(examId), payload);
  } catch {
    // Session persistence is best effort; backend remains source of truth
  }
};

export const getExamSession = (examId) => {
  if (!examId) return null;
  try {
    const raw = sessionStorage.getItem(getExamSessionKey(examId));
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (parsed && parsed.attemptId) {
      return {
        examId: parseInt(parsed.examId || examId, 10),
        attemptId: parseInt(parsed.attemptId, 10),
      };
    }
    return null;
  } catch {
    return null;
  }
};

export const clearExamSession = (examId) => {
  if (!examId) return;
  try {
    sessionStorage.removeItem(getExamSessionKey(examId));
  } catch {
    // ignore
  }
};
