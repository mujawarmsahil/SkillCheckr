import apiClient from "./client";

export const startExamAttempt = async (examId) => {
  const res = await apiClient.post(`/api/exams/${examId}/attempts`);
  return res.data;
};

export const saveAttemptAnswer = async (examId, attemptId, questionId, payload) => {
  const res = await apiClient.put(
    `/api/exams/${examId}/attempts/${attemptId}/answers/${questionId}`,
    payload
  );
  return res.data;
};

export const getAttemptAnswers = async (examId, attemptId) => {
  const res = await apiClient.get(
    `/api/exams/${examId}/attempts/${attemptId}/answers`
  );
  return Array.isArray(res.data) ? res.data : [];
};

export const submitExamAttempt = async (examId, attemptId) => {
  const res = await apiClient.post(
    `/api/exams/${examId}/attempts/${attemptId}/submit`
  );
  return res.data;
};
