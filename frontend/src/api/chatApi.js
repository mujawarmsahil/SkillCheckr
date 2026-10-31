import apiClient from "./client";

/**
 * Sends a chat message to the exam support assistant. When an exam or question context is
 * available it is forwarded so the assistant can stay anchored to the caller's situation.
 */
export const sendChatMessage = async (message, payload = {}) => {
  const res = await apiClient.post("/api/chat", { message, ...payload });
  const data = res.data;
  return {
    answer: data?.answer ?? null,
    answered: Boolean(data?.answered),
  };
};
