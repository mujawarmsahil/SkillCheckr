import apiClient from "./client";

export const getExamById = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}`);
  return res.data;
};

export const getExamQuestions = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}/questions`);
  return Array.isArray(res.data) ? res.data : [];
};

export const getUpcomingExams = async () => {
  const res = await apiClient.get("/api/exams/viewAllUpComingExam");
  return Array.isArray(res.data) ? res.data : [];
};

export const getAllExams = async () => {
  const res = await apiClient.get("/api/exams/viewAllExams");
  return Array.isArray(res.data) ? res.data : [];
};

export const registerForExam = async (examId, studentId) => {
  const res = await apiClient.post(`/api/exams/${examId}/register`, {
    studentId: parseInt(studentId, 10),
    examId: parseInt(examId, 10),
  });
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
