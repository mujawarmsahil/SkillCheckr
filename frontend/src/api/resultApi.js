import apiClient from "./client";

export const getStudentResults = async (studentId) => {
  const res = await apiClient.get(`/api/results/student/${studentId}`);
  return Array.isArray(res.data) ? res.data : [];
};

export const checkStudentExamStatus = async (examId, studentId) => {
  const res = await apiClient.get(`/api/results/check/${examId}/${studentId}`);
  return res.data;
};

export const getAllResults = async () => {
  const res = await apiClient.get("/api/results/all");
  return Array.isArray(res.data) ? res.data : [];
};

