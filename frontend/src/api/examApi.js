import apiClient from "./client";

const toCamelCase = (key) =>
  key.replace(/_([a-z])/g, (match, char) => char.toUpperCase());

/**
 * The teacher evaluation DTOs are serialized with snake_case properties, while the rest of
 * the UI works with camelCase keys (for example the question breakdown maps). Normalize the
 * payload once at the API boundary so components can use a single naming convention.
 */
const toCamelCaseKeys = (payload) => {
  if (Array.isArray(payload)) {
    return payload.map(toCamelCaseKeys);
  }
  if (payload === null || typeof payload !== "object") {
    return payload;
  }
  return Object.entries(payload).reduce((acc, [key, value]) => {
    acc[toCamelCase(key)] = toCamelCaseKeys(value);
    return acc;
  }, {});
};

export const getExamById = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}`);
  return res.data;
};

export const getExamQuestions = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}/questions`);
  return Array.isArray(res.data) ? res.data : [];
};

export const getUpcomingExams = async () => {
  const res = await apiClient.get("/api/exams/upcoming");
  return Array.isArray(res.data) ? res.data : [];
};

export const getAllExams = async () => {
  const res = await apiClient.get("/api/exams");
  return Array.isArray(res.data) ? res.data : [];
};

export const getTeacherExams = async (teacherId) => {
  const res = await apiClient.get(`/api/exams/teacher/${teacherId}`);
  return Array.isArray(res.data) ? res.data : [];
};

export const approveExam = async (examId) => {
  const res = await apiClient.post(`/api/exams/${examId}/approve`);
  return res.data;
};

export const rejectExam = async (examId) => {
  const res = await apiClient.post(`/api/exams/${examId}/reject`);
  return res.data;
};

export const cancelExam = async (examId) => {
  const res = await apiClient.post(`/api/exams/${examId}/cancel`);
  return res.data;
};

export const deleteExam = async (examId) => {
  const res = await apiClient.delete(`/api/exams/${examId}`);
  return res.data;
};

export const registerForExam = async (examId) => {
  const res = await apiClient.post(`/api/exams/${examId}/register`);
  return res.data;
};

export const checkStudentRegistration = async (examId, studentId) => {
  const res = await apiClient.get(`/api/exams/${examId}/isRegistered/${studentId}`);
  return res.data;
};

export const getStudentRegistrations = async (studentId) => {
  const res = await apiClient.get(`/api/exams/registrations/student/${studentId}`);
  return Array.isArray(res.data) ? res.data : [];
};

export const getExamSubmissions = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}/submissions`);
  return Array.isArray(res.data) ? toCamelCaseKeys(res.data) : [];
};

export const getEvaluationItems = async (examId, attemptId) => {
  const res = await apiClient.get(`/api/exams/${examId}/attempts/${attemptId}/evaluation`);
  return Array.isArray(res.data) ? toCamelCaseKeys(res.data) : [];
};

export const awardAnswerMarks = async (examId, attemptId, questionId, marks) => {
  const res = await apiClient.put(
    `/api/exams/${examId}/attempts/${attemptId}/answers/${questionId}/marks`,
    { marks }
  );
  return res.data;
};
