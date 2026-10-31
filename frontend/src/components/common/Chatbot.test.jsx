import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Chatbot from "./Chatbot";
import { sendChatMessage } from "../../api/chatApi";

vi.mock("./Icons", () => ({
  Icon: () => null,
}));

vi.mock("../../api/chatApi", () => ({
  sendChatMessage: vi.fn(),
}));

vi.mock("../../context/ToastContext", () => ({
  useToast: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarning: vi.fn(),
    showInfo: vi.fn(),
  }),
}));

const GREETING =
  "Hi! I am the SkillCheckr AI assistant. Ask me how to register for, take, or review exams.";

describe("Chatbot", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("opens the assistant and greets the user", async () => {
    render(<Chatbot />);

    expect(screen.queryByText(GREETING)).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /open ai assistant/i }));

    expect(await screen.findByText(GREETING)).toBeTruthy();
  });

  it("sends a message with the exam context and renders the answer", async () => {
    sendChatMessage.mockResolvedValue({ answer: "The answer is 4.", answered: true });

    render(<Chatbot context={{ examId: 5, questionId: 9 }} />);

    fireEvent.click(screen.getByRole("button", { name: /open ai assistant/i }));
    const input = await screen.findByPlaceholderText("Ask a question...");
    fireEvent.change(input, { target: { value: "What is 2+2?" } });
    fireEvent.click(screen.getByRole("button", { name: /send/i }));

    await waitFor(() => {
      expect(sendChatMessage).toHaveBeenCalledWith("What is 2+2?", {
        examId: 5,
        questionId: 9,
      });
    });
    expect(await screen.findByText("The answer is 4.")).toBeTruthy();
  });

  it("renders a fallback message when the assistant cannot answer", async () => {
    sendChatMessage.mockResolvedValue({ answer: null, answered: false });

    render(<Chatbot context={{ examId: 5, questionId: 9 }} />);

    fireEvent.click(screen.getByRole("button", { name: /open ai assistant/i }));
    const input = await screen.findByPlaceholderText("Ask a question...");
    fireEvent.change(input, { target: { value: "Help me" } });
    fireEvent.click(screen.getByRole("button", { name: /send/i }));

    expect(
      await screen.findByText("I could not answer that right now. Please try again later.")
    ).toBeTruthy();
  });

  it("renders the error message when sending fails", async () => {
    sendChatMessage.mockRejectedValue(new Error("Network down"));

    render(<Chatbot />);

    fireEvent.click(screen.getByRole("button", { name: /open ai assistant/i }));
    const input = await screen.findByPlaceholderText("Ask a question...");
    fireEvent.change(input, { target: { value: "Hello" } });
    fireEvent.click(screen.getByRole("button", { name: /send/i }));

    expect(await screen.findByText("Network down")).toBeTruthy();
  });
});
