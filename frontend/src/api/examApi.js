import apiClient from "./client";

export const getExamById = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}`);
  return res.data;
};

export const getExamQuestions = async (examId) => {
  const res = await apiClient.get(`/api/exams/${examId}/questions`);
  return res.data;
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
  return res.data;
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
