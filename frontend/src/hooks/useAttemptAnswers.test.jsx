import "@testing-library/react";
import { renderHook, act } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useAttemptAnswers } from "./useAttemptAnswers";

vi.mock("../api/attemptApi", () => ({
  saveAttemptAnswer: vi.fn(),
}));

import { saveAttemptAnswer } from "../api/attemptApi";

const DEBOUNCE_MS = 500;

const defaultArgs = {
  examId: 5,
  attemptId: 77,
  questions: [{ question_id: 1 }, { question_id: 2 }],
  isAttemptExpired: false,
  isMcqExam: false,
};

function renderAnswers(overrides = {}) {
  const showError = vi.fn();
  const view = renderHook(() => useAttemptAnswers({ ...defaultArgs, showError, ...overrides }));
  return { ...view, showError };
}

function payloads() {
  return saveAttemptAnswer.mock.calls.map(([, , questionId, payload]) => ({ questionId, payload }));
}

/**
 * Drains the microtask queue without touching the fake clock. RTL's waitFor cannot be used here
 * because it schedules its own polling timers, which the fake clock never advances.
 */
async function drain() {
  for (let i = 0; i < 20; i += 1) {
    await Promise.resolve();
  }
}

async function actDrain(fn) {
  await act(async () => {
    fn();
    await drain();
  });
}

beforeEach(() => {
  vi.useFakeTimers();
  saveAttemptAnswer.mockReset();
  saveAttemptAnswer.mockResolvedValue({});
});

afterEach(() => {
  vi.useRealTimers();
});

describe("debounced text saves", () => {
  it("does not save before the debounce elapses", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "my answer");
    });

    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS - 1));
    expect(saveAttemptAnswer).not.toHaveBeenCalled();
  });

  it("saves the latest value once the debounce elapses", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "m");
    });
    await actDrain(() => vi.advanceTimersByTime(100));
    act(() => {
      result.current.handleTextAnswerChange(1, "my final answer");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));

    expect(payloads()).toEqual([
      { questionId: 1, payload: { textAnswer: "my final answer" } },
    ]);
  });

  it("saves only the newest value when several keystrokes are debounced", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "a");
      result.current.handleTextAnswerChange(1, "ab");
      result.current.handleTextAnswerChange(1, "abc");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS * 2));

    expect(payloads()).toEqual([{ questionId: 1, payload: { textAnswer: "abc" } }]);
  });

  it("clears the debounce when the text is emptied and saves the clear immediately", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "typed");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));
    expect(payloads()).toHaveLength(1);

    await actDrain(() => result.current.handleTextAnswerChange(1, "   "));

    expect(payloads()).toEqual([
      { questionId: 1, payload: { textAnswer: "typed" } },
      { questionId: 1, payload: {} },
    ]);
  });

  it("does not send a stale debounced value after a clear", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "typed");
    });
    act(() => {
      result.current.handleTextAnswerChange(1, "");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS * 2));

    expect(payloads()).toEqual([{ questionId: 1, payload: {} }]);
  });
});

describe("flushPendingSaves", () => {
  it("saves a pending answer that the debounce has not fired for yet", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "typed then submitted");
    });
    expect(saveAttemptAnswer).not.toHaveBeenCalled();

    let ok;
    await actDrain(async () => {
      ok = await result.current.flushPendingSaves();
    });

    expect(ok).toBe(true);
    expect(payloads()).toEqual([
      { questionId: 1, payload: { textAnswer: "typed then submitted" } },
    ]);
  });

  it("cancels the pending debounce so the value is not saved twice", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "typed");
    });
    await actDrain(async () => {
      await result.current.flushPendingSaves();
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS * 3));

    expect(payloads()).toHaveLength(1);
  });

  it("flushes every pending question, not just the last one", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "first");
      result.current.handleTextAnswerChange(2, "second");
    });
    await actDrain(async () => {
      await result.current.flushPendingSaves();
    });

    expect(payloads()).toEqual([
      { questionId: 1, payload: { textAnswer: "first" } },
      { questionId: 2, payload: { textAnswer: "second" } },
    ]);
  });

  it("reports failure instead of success when one of the answers cannot be saved", async () => {
    saveAttemptAnswer.mockImplementation(async (examId, attemptId, questionId) => {
      if (questionId === 2) throw new Error("network down");
      return {};
    });
    const { result, showError } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "first");
      result.current.handleTextAnswerChange(2, "second");
    });

    let ok;
    await actDrain(async () => {
      ok = await result.current.flushPendingSaves();
    });

    expect(ok).toBe(false);
    expect(showError).toHaveBeenCalled();
  });

  it("is a no-op when nothing is pending", async () => {
    const { result } = renderAnswers();

    let ok;
    await actDrain(async () => {
      ok = await result.current.flushPendingSaves();
    });

    expect(ok).toBe(true);
    expect(saveAttemptAnswer).not.toHaveBeenCalled();
  });
});

describe("in-flight saves during a flush", () => {
  it("waits for a save that is still running before reporting success", async () => {
    const releases = [];
    saveAttemptAnswer.mockImplementation(
      () => new Promise((resolve) => {
        releases.push(() => resolve({}));
      })
    );
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "first");
    });
    // Let the debounce dispatch, leaving the save running.
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));
    expect(saveAttemptAnswer).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.handleTextAnswerChange(2, "second");
    });

    let settled = false;
    let ok;
    await actDrain(() => {
      result.current.flushPendingSaves().then((value) => {
        ok = value;
        settled = true;
      });
    });

    // The flush must not resolve while the first save is still open. The second question's save
    // is dispatched straight away because it is a different question, so only the first one blocks.
    expect(settled).toBe(false);
    expect(payloads()).toHaveLength(2);
    expect(releases).toHaveLength(2);

    await actDrain(() => releases.forEach((release) => release()));

    expect(settled).toBe(true);
    expect(ok).toBe(true);
    expect(payloads()).toEqual([
      { questionId: 1, payload: { textAnswer: "first" } },
      { questionId: 2, payload: { textAnswer: "second" } },
    ]);
  });

  it("serialises two saves for the same question so the newer value is not overwritten", async () => {
    const order = [];
    let releaseFirst;
    saveAttemptAnswer.mockImplementationOnce(
      () => new Promise((resolve) => {
        order.push("old-started");
        releaseFirst = () => {
          order.push("old-finished");
          resolve({});
        };
      })
    );
    saveAttemptAnswer.mockImplementationOnce(async () => {
      order.push("new-started");
      return {};
    });

    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "old");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));

    act(() => {
      result.current.handleTextAnswerChange(1, "new");
    });
    let flushed = false;
    act(() => {
      result.current.flushPendingSaves().then(() => {
        flushed = true;
      });
    });
    // The "new" save must be queued behind the still-running "old" save.
    expect(order).toEqual(["old-started"]);

    await actDrain(() => releaseFirst());
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));

    // The stale "old" save finished before the "new" save was even started, so the newer value
    // cannot be clobbered by it.
    expect(order).toEqual(["old-started", "old-finished", "new-started"]);
    expect(flushed).toBe(true);
    expect(payloads().at(-1)).toEqual({ questionId: 1, payload: { textAnswer: "new" } });
  });
});

describe("saves are suspended after submission", () => {
  it("does not send a save request after a successful submission", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.handleTextAnswerChange(1, "typed");
    });
    await actDrain(async () => {
      await result.current.flushPendingSaves();
    });
    const callsAtSubmit = saveAttemptAnswer.mock.calls.length;

    act(() => {
      result.current.suspendSaves();
    });
    // Anything still scheduled, or typed by a stale event, must be dropped.
    await actDrain(() => {
      result.current.handleTextAnswerChange(1, "typed after submit");
      vi.advanceTimersByTime(DEBOUNCE_MS * 3);
    });

    expect(saveAttemptAnswer.mock.calls.length).toBe(callsAtSubmit);
  });

  it("keeps saving again after resumeSaves, so a failed submission is recoverable", async () => {
    const { result } = renderAnswers();

    act(() => {
      result.current.suspendSaves();
    });
    act(() => {
      result.current.handleTextAnswerChange(1, "dropped");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));
    expect(saveAttemptAnswer).not.toHaveBeenCalled();

    act(() => {
      result.current.resumeSaves();
    });
    act(() => {
      result.current.handleTextAnswerChange(1, "kept");
    });
    await actDrain(() => vi.advanceTimersByTime(DEBOUNCE_MS));

    expect(payloads()).toEqual([{ questionId: 1, payload: { textAnswer: "kept" } }]);
  });
});

describe("MCQ answers", () => {
  const mcqQuestion = {
    question_id: 9,
    option1: "A",
    option2: "B",
    options: [{ answerId: 100 }, { answerId: 200 }],
  };

  it("still saves immediately, without any debounce", async () => {
    const { result } = renderAnswers({ isMcqExam: true });

    await actDrain(() => result.current.handleSelectOption(mcqQuestion, "option1", "A"));

    expect(payloads()).toEqual([{ questionId: 9, payload: { selectedAnswerId: 100 } }]);
  });

  it("is waited for by a flush", async () => {
    let release;
    saveAttemptAnswer.mockImplementation(
      () => new Promise((resolve) => {
        release = () => resolve({});
      })
    );
    const { result } = renderAnswers({ isMcqExam: true });

    await actDrain(() => result.current.handleSelectOption(mcqQuestion, "option1", "A"));

    let settled = false;
    await actDrain(() => {
      result.current.flushPendingSaves().then(() => {
        settled = true;
      });
    });
    expect(settled).toBe(false);

    await actDrain(() => release());
    expect(settled).toBe(true);
  });
});
