import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import TakeExam from "./TakeExam";

const mocks = vi.hoisted(() => ({
  resultData: null,
  navigate: vi.fn(),
}));

vi.mock("react-router-dom", () => ({
  useParams: () => ({ examId: "5" }),
  useLocation: () => ({ state: null }),
  useNavigate: () => mocks.navigate,
}));

vi.mock("../../context/ToastContext", () => ({
  useToast: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarning: vi.fn(),
  }),
}));

vi.mock("../../hooks/useExamAttempt", () => ({
  useExamAttempt: () => ({
    exam: null,
    questions: [],
    attemptSession: null,
    loading: false,
    alreadySubmitted: true,
    resultData: mocks.resultData,
    setResultData: vi.fn(),
    accessBlocked: null,
    setAccessBlocked: vi.fn(),
    initialAnswers: [],
    loadExamAndAttempt: vi.fn(),
  }),
}));

vi.mock("../../hooks/useExamTimer", () => ({
  useExamTimer: () => ({
    timeLeftSeconds: 0,
    isAttemptExpired: false,
    formattedTime: "00:00",
  }),
}));

vi.mock("../../hooks/useAttemptAnswers", () => ({
  useAttemptAnswers: () => ({
    mcqAnswers: {},
    textAnswers: {},
    restoreSavedAnswers: vi.fn(),
    handleSelectOption: vi.fn(),
    handleTextAnswerChange: vi.fn(),
    answeredCount: 0,
    flushPendingSaves: vi.fn(),
    suspendSaves: vi.fn(),
    resumeSaves: vi.fn(),
  }),
}));

vi.mock("../../api/examApi", () => ({
  registerForExam: vi.fn(),
}));

vi.mock("../../api/attemptApi", () => ({
  submitExamAttempt: vi.fn(),
}));

vi.mock("../common/Icons", () => ({
  Icon: () => null,
}));

describe("TakeExam answer-key release", () => {
  beforeEach(() => {
    mocks.resultData = {
      status: "Pass",
      marks_obtained: 4,
      total_marks: 5,
      percentage: 80,
      answer_key_released: false,
      question_breakdown: [],
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("explains that detailed review is unavailable before release", () => {
    render(<TakeExam />);

    expect(
      screen.getByText("Detailed answer review will be available after the scheduled exam window closes.")
    ).toBeTruthy();
    expect(screen.queryByText(/Correct answer:/)).toBeNull();
  });

  it("shows the answer review after release", () => {
    mocks.resultData = {
      ...mocks.resultData,
      answer_key_released: true,
      question_breakdown: [
        {
          questionId: 12,
          question: "Which option is correct?",
          questionType: "MCQ",
          selectedAnswer: "A",
          correctAnswer: "B",
          isCorrect: false,
          marks: 1,
          awardedMarks: 0,
        },
      ],
    };

    render(<TakeExam />);

    expect(screen.queryByText(/Detailed answer review will be available/)).toBeNull();
    expect(screen.getByText("Correct answer: B")).toBeTruthy();
  });
});
