import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("user_id");
      window.dispatchEvent(new Event("auth:logout"));
    }

    let data = error.response?.data;
    if (data instanceof Blob) {
      try {
        const text = await data.text();
        try {
          data = JSON.parse(text);
        } catch {
          data = text;
        }
      } catch {
        data = null;
      }
    }

    const message =
      (typeof data === "object" && data !== null ? data.message : null) ||
      (typeof data === "string" ? data : null) ||
      error.message ||
      "An unexpected error occurred";
    const customError = new Error(typeof message === "string" ? message : JSON.stringify(message));
    customError.response = error.response;
    customError.status = error.response?.status;
    customError.code = error.code;
    return Promise.reject(customError);
  }
);

export default apiClient;
