import React, { useEffect, useState } from "react";
import { downloadExamQuestionBank } from "../../api/examApi";
import { Icon } from "./Icons";

const getFilename = (contentDisposition, examId) => {
  const filenameMatch = contentDisposition?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)/i);
  if (!filenameMatch?.[1]) return `question-bank-${examId}.pdf`;
  try {
    return decodeURIComponent(filenameMatch[1].trim());
  } catch {
    return `question-bank-${examId}.pdf`;
  }
};

export default function QuestionBankViewer({ exam, onClose }) {
  const examId = exam.exam_id || exam.examId;
  const examName = exam.exam_name || exam.examName || "Question bank";
  const [pdfUrl, setPdfUrl] = useState("");
  const [filename, setFilename] = useState(`question-bank-${examId}.pdf`);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isActive = true;
    let objectUrl = "";

    const loadPdf = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await downloadExamQuestionBank(examId);
        if (!isActive) return;
        objectUrl = window.URL.createObjectURL(
          new Blob([response.data], { type: "application/pdf" })
        );
        setPdfUrl(objectUrl);
        setFilename(getFilename(response.headers["content-disposition"], examId));
      } catch (requestError) {
        if (isActive) {
          setError(requestError.message || "Could not load the question bank PDF.");
        }
      } finally {
        if (isActive) setLoading(false);
      }
    };

    loadPdf();
    return () => {
      isActive = false;
      if (objectUrl) window.URL.revokeObjectURL(objectUrl);
    };
  }, [examId]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-3 sm:p-6"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="question-bank-title"
        className="flex max-h-[95vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
      >
        <header className="flex items-center justify-between gap-4 border-b border-slate-200 px-4 py-3 sm:px-6">
          <div className="min-w-0">
            <h2 id="question-bank-title" className="truncate text-base font-bold text-slate-900">
              {examName} — Question Bank
            </h2>
            <p className="text-xs text-slate-500">PDF preview</p>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            {pdfUrl && (
              <a
                href={pdfUrl}
                download={filename}
                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-50 px-3 py-2 text-xs font-semibold text-indigo-800 hover:bg-indigo-100"
              >
                <Icon name="file-text" className="h-4 w-4" />
                Download
              </a>
            )}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close question bank preview"
              className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
            >
              <Icon name="x" className="h-5 w-5" />
            </button>
          </div>
        </header>

        <div className="min-h-72 flex-1 bg-slate-100">
          {loading && (
            <div className="flex h-72 items-center justify-center text-sm text-slate-600" role="status">
              Loading question bank PDF...
            </div>
          )}
          {error && (
            <div className="flex h-72 items-center justify-center px-6 text-center text-sm text-rose-700" role="alert">
              {error}
            </div>
          )}
          {!loading && !error && pdfUrl && (
            <iframe
              title={`${examName} question bank PDF`}
              src={pdfUrl}
              className="h-[75vh] min-h-72 w-full border-0"
            />
          )}
        </div>
      </section>
    </div>
  );
}
