import React, { useState, useEffect, useCallback } from "react";
import {
  getExamSubmissions,
  getEvaluationItems,
  awardAnswerMarks,
} from "../../api/examApi";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { RESULT_STATUS, ATTEMPT_STATUS } from "../../constants/resultConstants";

export default function EvaluateSubmission({ examId, onClose }) {
  const { showSuccess, showError } = useToast();

  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedAttempt, setSelectedAttempt] = useState(null);
  const [items, setItems] = useState([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [savingQuestionId, setSavingQuestionId] = useState(null);
  const [draftMarks, setDraftMarks] = useState({});

  const fetchSubmissions = useCallback(async () => {
    setLoading(true);
    try {
      setSubmissions(await getExamSubmissions(examId));
    } catch (err) {
      showError(err.message || "Failed to load submissions");
      setSubmissions([]);
    } finally {
      setLoading(false);
    }
  }, [examId, showError]);

  useEffect(() => {
    fetchSubmissions();
  }, [fetchSubmissions]);

  const openEvaluation = async (submission) => {
    const attemptId = submission.attemptId;
    setSelectedAttempt({ attemptId, submission });
    setLoadingItems(true);
    setItems([]);
    try {
      setItems(await getEvaluationItems(examId, attemptId));
    } catch (err) {
      showError(err.message || "Failed to load the submitted answers");
    } finally {
      setLoadingItems(false);
    }
  };

  const handleSaveMarks = async (item) => {
    const questionId = item.questionId;
    const raw = draftMarks[questionId];
    const value = raw !== undefined ? parseFloat(raw) : item.awardedMarks;

    if (Number.isNaN(value) || value === null || value === undefined) {
      showError("Enter a numeric mark value.");
      return;
    }
    // Marks are whole numbers end to end. Rejecting a fractional value here keeps the
    // teacher from being told a mark was saved when the server would reject or round it.
    if (!Number.isInteger(value)) {
      showError("Marks must be a whole number.");
      return;
    }
    if (value < 0 || value > item.marks) {
      showError(`Marks must be between 0 and ${item.marks}.`);
      return;
    }

    setSavingQuestionId(questionId);
    try {
      await awardAnswerMarks(examId, selectedAttempt.attemptId, questionId, value);
      setItems((prev) =>
        prev.map((row) =>
          row.questionId === questionId
            ? { ...row, awardedMarks: value, isEvaluated: true }
            : row
        )
      );
      showSuccess(`Marks awarded for question ${item.questionOrder}.`);
      await fetchSubmissions();
    } catch (err) {
      showError(err.message || "Failed to award marks");
    } finally {
      setSavingQuestionId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[85vh] flex flex-col overflow-hidden border border-slate-200">
        <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="font-bold text-slate-800 text-base flex items-center gap-2">
              <Icon name="file-text" className="w-4 h-4 text-orange-500" />
              Submissions &amp; Grading
            </h3>
            <p className="text-xs text-slate-500">
              Written answers are graded here. Every mark is stored on the server and the student result
              is recalculated immediately.
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg"
          >
            <Icon name="x" className="w-5 h-5" />
          </button>
        </div>

        {selectedAttempt ? (
          <>
            <div className="px-4 py-2.5 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
              <div className="text-xs text-slate-600">
                <span className="font-bold text-slate-800">
                  {selectedAttempt.submission.studentName || `Student #${selectedAttempt.submission.studentId}`}
                </span>
                <span className="text-slate-400"> • </span>
                Attempt #{selectedAttempt.attemptId}
                <span className="text-slate-400"> • </span>
                {selectedAttempt.submission.marksObtained} / {selectedAttempt.submission.totalMarks}
              </div>
              <button
                onClick={() => setSelectedAttempt(null)}
                className="text-xs font-semibold text-orange-600 hover:text-orange-700"
              >
                ← All submissions
              </button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 space-y-4">
              {loadingItems ? (
                <div className="py-10 text-center">
                  <div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  <p className="text-xs text-slate-500">Loading answers...</p>
                </div>
              ) : items.length === 0 ? (
                <p className="text-center text-sm text-slate-500 py-6">
                  This attempt has no stored answers to grade.
                </p>
              ) : (
                items.map((item) => {
                  const isMcq = (item.questionType || "").toUpperCase() === "MCQ";
                  const needsGrading =
                    item.requiresEvaluation ?? !isMcq;

                  return (
                    <div
                      key={item.questionId}
                      className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-3"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className="text-sm font-semibold text-slate-800">
                          <span className="text-orange-600 font-bold mr-1.5">
                            Q{item.questionOrder || item.questionId}.
                          </span>
                          {item.question}
                        </p>
                        <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-slate-200 text-slate-700 whitespace-nowrap">
                          {item.awardedMarks ?? 0} / {item.marks}
                        </span>
                      </div>

                      {isMcq ? (
                        <div className="text-xs text-slate-600 space-y-1">
                          <p>
                            Selected:{" "}
                            <span className="font-semibold text-slate-800">
                              {item.selectedAnswer || "No answer"}
                            </span>
                          </p>
                          <p className="text-slate-400">
                            Marked automatically — descriptive grading is not available for MCQ.
                          </p>
                        </div>
                      ) : (
                        <>
                          <div className="text-xs text-slate-600">
                            <span className="font-bold text-slate-700">Student answer:</span>
                            <p className="mt-1 p-2.5 bg-white rounded-lg border border-slate-200 whitespace-pre-wrap">
                              {item.textAnswer || "No answer provided"}
                            </p>
                          </div>

                          {needsGrading && (
                            <div className="flex items-end gap-2">
                              <div>
                                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                                  Marks (max {item.marks})
                                </label>
                                <input
                                  type="number"
                                  min="0"
                                  max={item.marks}
                                  step="1"
                                  value={
                                    draftMarks[item.questionId] !== undefined
                                      ? draftMarks[item.questionId]
                                      : (item.awardedMarks ?? 0)
                                  }
                                  onChange={(e) =>
                                    setDraftMarks((prev) => ({
                                      ...prev,
                                      [item.questionId]: e.target.value,
                                    }))
                                  }
                                  className="w-24 px-2.5 py-1.5 bg-white border border-slate-300 rounded-lg text-sm outline-none focus:border-orange-500"
                                />
                              </div>
                              <button
                                onClick={() => handleSaveMarks(item)}
                                disabled={savingQuestionId === item.questionId}
                                className="py-2 px-4 bg-slate-900 hover:bg-slate-800 disabled:opacity-60 text-white text-xs font-bold rounded-lg transition-all"
                              >
                                {savingQuestionId === item.questionId ? "Saving..." : "Save Marks"}
                              </button>
                              {item.isEvaluated ? (
                                <span className="text-[11px] text-emerald-600 font-semibold pb-2">
                                  Graded
                                </span>
                              ) : (
                                <span className="text-[11px] text-amber-600 font-semibold pb-2">
                                  Awaiting grading
                                </span>
                              )}
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  );
                })
              )}
            </div>

            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={onClose}
                className="py-2 px-4 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-xl"
              >
                Close
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="p-5 overflow-y-auto flex-1">
              {loading ? (
                <div className="py-10 text-center">
                  <div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  <p className="text-xs text-slate-500">Loading submissions...</p>
                </div>
              ) : submissions.length === 0 ? (
                <p className="text-center text-sm text-slate-500 py-6">
                  No student has submitted this exam yet.
                </p>
              ) : (
                <table className="w-full text-left border-collapse text-sm">
                  <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                    <tr>
                      <th className="py-2.5 px-3">Student</th>
                      <th className="py-2.5 px-3">Score</th>
                      <th className="py-2.5 px-3">Result</th>
                      <th className="py-2.5 px-3">Submitted</th>
                      <th className="py-2.5 px-3 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {submissions.map((s) => {
                      const isPending = Boolean(s.pendingEvaluation);
                      const isInProgress = s.attemptStatus === ATTEMPT_STATUS.IN_PROGRESS;
                      const status =
                        s.resultStatus ||
                        (isInProgress
                          ? "In Progress"
                          : RESULT_STATUS.SUBMITTED_FOR_EVALUATION);
                      const tone = isInProgress
                        ? "bg-slate-100 text-slate-700"
                        : isPending
                          ? "bg-amber-100 text-amber-800"
                          : "bg-emerald-100 text-emerald-800";

                      return (
                        <tr key={s.attemptId} className="hover:bg-slate-50/70 transition-colors">
                          <td className="py-2.5 px-3">
                            <div className="font-semibold text-slate-900">
                              {s.studentName || `Student #${s.studentId}`}
                            </div>
                            <div className="text-[11px] text-slate-400">{s.studentEmail}</div>
                          </td>
                          <td className="py-2.5 px-3 font-mono text-xs">
                            {s.marksObtained} / {s.totalMarks}
                            <span className="text-slate-400"> ({Math.round(s.percentage)}%)</span>
                          </td>
                          <td className="py-2.5 px-3">
                            <span
                              className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${tone}`}
                            >
                              {status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-[11px] text-slate-500">
                            {s.submittedAt ? String(s.submittedAt).split("T")[0] : "In progress"}
                          </td>
                          <td className="py-2.5 px-3 text-right">
                            {isInProgress ? (
                              <span className="text-[11px] text-slate-400">Not submitted</span>
                            ) : (
                              <button
                                onClick={() => openEvaluation(s)}
                                className="px-2.5 py-1 text-xs font-semibold text-orange-600 hover:text-orange-700 hover:bg-orange-50 rounded-lg transition-colors inline-flex items-center gap-1"
                              >
                                <Icon name="eye" className="w-3.5 h-3.5" />
                                {isPending ? "Grade" : "Review"}
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={onClose}
                className="py-2 px-4 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-xl"
              >
                Close
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
