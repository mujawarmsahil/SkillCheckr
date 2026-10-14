import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { downloadExamQuestionBank } from "../../api/examApi";
import QuestionBankViewer from "./QuestionBankViewer";

vi.mock("../../api/examApi", () => ({
  downloadExamQuestionBank: vi.fn(),
}));

describe("QuestionBankViewer", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("previews and offers download for the authorized exam question PDF", async () => {
    const createObjectURL = vi.fn(() => "blob:question-bank");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", { createObjectURL, revokeObjectURL });
    downloadExamQuestionBank.mockResolvedValue({
      data: new Blob(["pdf"]),
      headers: { "content-disposition": 'attachment; filename="questions.pdf"' },
    });
    const onClose = vi.fn();
    const view = render(
      <QuestionBankViewer
        exam={{ exam_id: 12, exam_name: "Biology Final" }}
        onClose={onClose}
      />
    );

    expect((await screen.findByTitle("Biology Final question bank PDF")).getAttribute("src"))
      .toBe("blob:question-bank");
    expect(downloadExamQuestionBank).toHaveBeenCalledWith(12);
    expect(screen.getByRole("link", { name: /download/i }).getAttribute("download"))
      .toBe("questions.pdf");

    fireEvent.click(screen.getByRole("button", { name: "Close question bank preview" }));
    expect(onClose).toHaveBeenCalledOnce();
    view.unmount();
    await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith("blob:question-bank"));
  });
});
