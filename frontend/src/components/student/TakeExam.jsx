import React, { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { registerForExam } from "../../api/examApi";
import { submitExamAttempt } from "../../api/attemptApi";
import { QUESTION_TYPES } from "../../constants/examConstants";
import { RESULT_STATUS } from "../../constants/resultConstants";
import { useExamAttempt } from "../../hooks/useExamAttempt";
import { useExamTimer } from "../../hooks/useExamTimer";
import { useAttemptAnswers } from "../../hooks/useAttemptAnswers";

export default function TakeExam() {
  const { examId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { showSuccess, showError, showWarning } = useToast();

  const [currentIndex, setCurrentIndex] = useState(0);
  const [flagged, setFlagged] = useState(new Set());
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);

  const isSubmittingRef = useRef(false);

  const {
    exam,
    questions,
    attemptSession,
    loading,
    alreadySubmitted,
    resultData,
    setResultData,
    accessBlocked,
    setAccessBlocked,
    initialAnswers,
    loadExamAndAttempt,
  } = useExamAttempt({
    examId,
    locationState: location.state,
    showError,
  });

  const isMcq =
    (exam?.exam_type || exam?.examType || questions[currentIndex]?.questionType || QUESTION_TYPES.MCQ).toUpperCase() ===
    QUESTION_TYPES.MCQ;

  // useExamTimer, useAttemptAnswers and submitExam reference each other: the timer needs
  // submitExam, submitExam needs flushPendingSaves, and useAttemptAnswers needs isAttemptExpired
  // from the timer. The ref breaks the cycle so the hooks can be declared in dependency order.
  const submitExamRef = useRef(null);

  const { timeLeftSeconds, isAttemptExpired, formattedTime } = useExamTimer({
    expiresAt: attemptSession?.expiresAt,
    onExpire: () => submitExamRef.current?.(),
    active: !resultData && !loading,
    showWarning,
  });

  const {
    mcqAnswers,
    textAnswers,
    restoreSavedAnswers,
    handleSelectOption,
    handleTextAnswerChange,
    answeredCount,
    flushPendingSaves,
    suspendSaves,
    resumeSaves,
  } = useAttemptAnswers({
    examId,
    attemptId: attemptSession?.attemptId,
    questions,
    isAttemptExpired,
    showError,
    isMcqExam: isMcq,
  });

  const submitExam = useCallback(async () => {
    if (isSubmittingRef.current) return;
    isSubmittingRef.current = true;
    setIsSubmitting(true);
    setShowSubmitModal(false);

    try {
      const activeAttemptId = attemptSession?.attemptId;
      if (!activeAttemptId) {
        throw new Error("Exam attempt is not ready. Reload the page and try again.");
      }

      // Scoring, expiry and the final status are decided by the server.
      // Nothing may be submitted while an answer is still unsaved: the server rejects an answer
      // for an attempt that is already submitted, so a late save would silently lose the text.
      const answersSaved = await flushPendingSaves();
      if (!answersSaved) {
        showError("Some answers could not be saved, so the exam was not submitted. Please try again.");
        return;
      }

      suspendSaves();
      const result = await submitExamAttempt(examId, activeAttemptId);

      setResultData(result);
      showSuccess("Exam submitted.");
    } catch (err) {
      // The attempt is still open, so the student has to be able to keep editing and saving.
      resumeSaves();
      showError(err.message || "Failed to submit exam.");
    } finally {
      isSubmittingRef.current = false;
      setIsSubmitting(false);
    }
  }, [
    attemptSession?.attemptId,
    examId,
    flushPendingSaves,
    resumeSaves,
    showError,
    showSuccess,
    setResultData,
    suspendSaves,
  ]);

  useEffect(() => {
    submitExamRef.current = submitExam;
  }, [submitExam]);

  useEffect(() => {
    if (initialAnswers.length > 0 && questions.length > 0) {
      restoreSavedAnswers(initialAnswers, questions);
    }
  }, [initialAnswers, questions, restoreSavedAnswers]);

  useEffect(() => {
    loadExamAndAttempt();
  }, [loadExamAndAttempt]);

  const currentQuestion = questions[currentIndex];
  const currentQuestionId = currentQuestion?.question_id || currentQuestion?.questionId || currentIndex + 1;

  const toggleFlag = () => {
    setFlagged((prev) => {
      const next = new Set(prev);
      if (next.has(currentQuestionId)) {
        next.delete(currentQuestionId);
      } else {
        next.add(currentQuestionId);
      }
      return next;
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
        <div className="w-12 h-12 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-sm font-bold text-slate-200">Loading exam...</p>
      </div>
    );
  }

  if (accessBlocked) {
    const isNotRegistered = accessBlocked.reason === "NOT_REGISTERED";
    const isNotStarted = accessBlocked.reason === "NOT_STARTED";

    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-4 sm:p-6 select-none">
        <div className="w-full max-w-lg bg-white text-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
          <div
            className={`p-8 text-center ${
              isNotRegistered ? "bg-slate-900 text-white" : isNotStarted ? "bg-blue-600 text-white" : "bg-rose-600 text-white"
            }`}
          >
            <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-3">
              <Icon name={isNotRegistered ? "lock" : isNotStarted ? "clock" : "alert-triangle"} className="w-8 h-8 text-white" />
            </div>
            <h2 className="text-2xl font-black">
              {isNotRegistered
                ? "Registration Required"
                : isNotStarted
                ? "Exam Not Started"
                : "Exam Window Closed"}
            </h2>
            <p className="text-xs text-white/90 mt-1">
              {exam?.exam_name || `Exam #${examId}`} • {exam?.subject?.subject_name || "General"}
            </p>
          </div>

          <div className="p-6 space-y-5 text-center">
            <p className="text-sm text-slate-600 leading-relaxed font-medium">
              {accessBlocked.message}
            </p>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 space-y-2 text-xs text-slate-600">
              <div className="flex justify-between">
                <span className="font-semibold text-slate-500">Scheduled Date:</span>
                <span className="font-bold text-slate-800">{accessBlocked.datePart}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-semibold text-slate-500">Time Window:</span>
                <span className="font-mono font-bold text-slate-800">
                  {accessBlocked.startTimeStr} - {accessBlocked.endTimeStr}
                </span>
              </div>
            </div>

            <div className="flex flex-col gap-3 pt-2">
              {isNotRegistered && accessBlocked.isRegistrationOpen && (
                <button
                  type="button"
                  disabled={isRegistering}
                  onClick={async () => {
                    setIsRegistering(true);
                    try {
                      await registerForExam(examId);
                      showSuccess("Registered. Opening exam...");
                      setAccessBlocked(null);
                      loadExamAndAttempt();
                    } catch (err) {
                      showError(err.message || "Failed to register for this exam");
                    } finally {
                      setIsRegistering(false);
                    }
                  }}
                  className="w-full py-3 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 disabled:opacity-60 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2"
                >
                  <Icon name="check-circle" className="w-4 h-4" />
                  <span>{isRegistering ? "Registering..." : "Register & Start Exam →"}</span>
                </button>
              )}

              <button
                type="button"
                onClick={() => navigate("/dashboard/student")}
                className="w-full py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-all"
              >
                Back to Available Exams
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (resultData) {
    const isPass = resultData.status === RESULT_STATUS.PASS;
    const isFail = resultData.status === RESULT_STATUS.FAIL;
    const isPendingEvaluation = resultData.status === RESULT_STATUS.SUBMITTED_FOR_EVALUATION;

    const marksObtained = resultData.marks_obtained ?? resultData.marksObtained ?? 0;
    const totalMarks = resultData.total_marks ?? resultData.totalMarks ?? 0;
    const percentage = resultData.percentage ?? 0;

    const examName =
      resultData.exam_name || resultData.examName || exam?.exam_name || `Exam #${examId}`;
    const subjectName =
      resultData.subject_name || resultData.subjectName || exam?.subject?.subject_name || "General";

    const breakdownList = resultData.question_breakdown || resultData.questionBreakdown || [];

    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-4 sm:p-6 select-none">
        <div className="w-full max-w-2xl bg-white text-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
          <div
            className={`p-8 text-center ${
              isPass ? "bg-emerald-600 text-white" : isFail ? "bg-rose-600 text-white" : "bg-slate-900 text-white"
            }`}
          >
            <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-3">
              <Icon name={isPass ? "check-circle" : isFail ? "x-circle" : "award"} className="w-8 h-8 text-white" />
            </div>
            <h2 className="text-2xl font-black">
              {alreadySubmitted
                ? "Exam Already Submitted"
                : isPass
                ? "Exam Passed"
                : isFail
                ? "Exam Not Passed"
                : "Exam Submitted"}
            </h2>
            <p className="text-xs text-white/90 mt-1">
              {examName} • {subjectName}
            </p>
          </div>

          {alreadySubmitted && (
            <div className="bg-amber-50 border-b border-amber-200 p-4 text-center">
              <p className="text-xs font-bold text-amber-800 uppercase tracking-wide">Single Attempt Policy</p>
              <p className="text-sm font-semibold text-amber-900 mt-1">
                You have already submitted this exam. Re-attempts are not allowed.
              </p>
            </div>
          )}

          {isPendingEvaluation && (
            <div className="bg-blue-50 border-b border-blue-200 p-4 text-center">
              <p className="text-xs font-bold text-blue-800 uppercase tracking-wide">Evaluation Pending</p>
              <p className="text-sm font-semibold text-blue-900 mt-1">
                Your written answers need teacher evaluation. The result is updated after grading.
              </p>
            </div>
          )}

          <div className="p-6 sm:p-8 space-y-6">
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Score</span>
                <p className="text-2xl font-extrabold text-slate-900 mt-1">
                  {marksObtained ?? "-"} <span className="text-xs font-normal text-slate-400">/ {totalMarks ?? "-"}</span>
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Percentage</span>
                <p className={`text-2xl font-extrabold mt-1 ${isPass ? "text-orange-600" : "text-slate-700"}`}>
                  {percentage ?? "-"}{percentage !== undefined && "%"}
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Outcome</span>
                <p className={`text-base font-bold mt-2 ${isPass ? "text-emerald-600" : isFail ? "text-rose-600" : "text-amber-600"}`}>
                  {resultData.status || "Submitted"}
                </p>
              </div>
            </div>

            {breakdownList.length > 0 && (
              <div className="space-y-3 pt-2">
                <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Question Review ({breakdownList.length})
                </h4>
                <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
                  {breakdownList.map((item, idx) => {
                    const isMcq = (item.questionType || "").toUpperCase() === "MCQ";
                    const selectedAnswer = isMcq ? item.selectedAnswer || "None" : item.textAnswer || "No answer";
                    const correctAnswer = item.correctAnswer || "";
                    const isItemCorrect = item.isCorrect;

                    return (
                      <div
                        key={item.questionId ?? idx}
                        className={`p-3 rounded-xl border text-xs space-y-1 ${
                          isItemCorrect === true
                            ? "bg-emerald-50/70 border-emerald-200"
                            : isItemCorrect === false
                            ? "bg-rose-50/70 border-rose-200"
                            : "bg-slate-50 border-slate-200"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <p className="font-semibold text-slate-800">
                            {idx + 1}. {item.question}
                          </p>
                          <span
                            className={`font-bold px-2 py-0.5 rounded text-[10px] whitespace-nowrap ${
                              isItemCorrect === true
                                ? "bg-emerald-200 text-emerald-800"
                                : isItemCorrect === false
                                ? "bg-rose-200 text-rose-800"
                                : "bg-slate-200 text-slate-700"
                            }`}
                          >
                            {isItemCorrect === true
                              ? "Correct"
                              : isItemCorrect === false
                              ? "Incorrect"
                              : `${item.awardedMarks ?? 0} / ${item.marks ?? 0} marks`}
                          </span>
                        </div>
                        <p className="text-slate-600">
                          Your answer: <strong className="text-slate-900">{selectedAnswer}</strong>
                        </p>
                        {isItemCorrect === false && correctAnswer && (
                          <p className="text-emerald-700 font-medium">Correct answer: {correctAnswer}</p>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-slate-100">
              <button
                onClick={() => navigate("/dashboard/student")}
                className="flex-1 py-3 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl text-sm transition-all text-center"
              >
                Return to Student Dashboard
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Loading finished without a result, an access block, or a live attempt: the attempt
  // could not be prepared. Offer a retry instead of rendering an empty question paper.
  if (!attemptSession) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-4">
        <div className="w-full max-w-md bg-white text-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200 text-center">
          <div className="p-8 bg-rose-600 text-white">
            <div className="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center mx-auto mb-3">
              <Icon name="alert-triangle" className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-black">Exam Unavailable</h2>
            <p className="text-xs text-white/90 mt-1">
              {exam?.exam_name || `Exam #${examId}`}
            </p>
          </div>
          <div className="p-6 space-y-3">
            <p className="text-sm text-slate-600">
              The exam attempt could not be prepared. This is usually a temporary connection
              problem.
            </p>
            <button
              type="button"
              onClick={() => loadExamAndAttempt()}
              className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-all"
            >
              Try Again
            </button>
            <button
              type="button"
              onClick={() => navigate("/dashboard/student")}
              className="w-full py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-all"
            >
              Back to Available Exams
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col select-none relative overflow-x-hidden">
      <header className="h-20 bg-slate-900/95 backdrop-blur-md border-b border-slate-800 px-4 sm:px-8 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-4">
          <button
            onClick={() => {
              if (window.confirm("Exit exam? Your saved answers are already stored on the server.")) {
                navigate("/dashboard/student");
              }
            }}
            className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition-colors text-xs flex items-center gap-1.5"
          >
            ← Exit
          </button>

          <div>
            <h1 className="text-base font-bold text-white leading-none truncate max-w-xs sm:max-w-md">
              {exam?.exam_name || exam?.examName}
            </h1>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-xs text-slate-400 font-medium">
                {exam?.subject?.subject_name || "General"} • {isMcq ? "MCQ Exam" : "Written Q&A"}
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 sm:gap-5">
          <div
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-mono font-black border transition-colors ${
              isAttemptExpired || timeLeftSeconds < 300
                ? "bg-rose-950/80 border-rose-500 text-rose-400 animate-pulse"
                : "bg-slate-800 border-slate-700 text-orange-400"
            }`}
          >
            <Icon name="clock" className="w-4 h-4" />
            <span>{isAttemptExpired ? "Expired" : formattedTime}</span>
          </div>

          <button
            onClick={() => setShowSubmitModal(true)}
            className="py-2.5 px-4 sm:px-5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs sm:text-sm font-black rounded-xl shadow-lg transition-all"
          >
            Finish &amp; Submit
          </button>
        </div>
      </header>

      <main className="flex-1 max-w-[1536px] w-full mx-auto p-4 sm:p-8 grid grid-cols-1 lg:grid-cols-4 gap-8">
        <div className="lg:col-span-3 flex flex-col justify-between bg-slate-900/60 border border-slate-800 rounded-3xl p-6 sm:p-10 backdrop-blur-sm space-y-8 relative">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <span className="text-xs font-black px-3 py-1.5 rounded-xl bg-orange-500 text-white shadow-sm">
                  Question {currentIndex + 1}
                </span>
                <span className="text-xs font-semibold text-slate-400">of {questions.length}</span>
                {currentQuestion?.marks && (
                  <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300">
                    {currentQuestion.marks} Marks
                  </span>
                )}
              </div>

              <button
                type="button"
                onClick={toggleFlag}
                className={`text-xs font-bold px-3.5 py-1.5 rounded-xl border transition-all flex items-center gap-1.5 ${
                  flagged.has(currentQuestionId)
                    ? "bg-amber-500/20 border-amber-500 text-amber-300 shadow-sm"
                    : "bg-slate-800/60 border-slate-700 text-slate-400 hover:text-white"
                }`}
              >
                ★ {flagged.has(currentQuestionId) ? "Flagged for Review" : "Flag for Review"}
              </button>
            </div>

            <h2 className="text-lg sm:text-xl font-bold text-slate-100 leading-relaxed">
              {currentQuestion?.question}
            </h2>
          </div>

          <div className="flex-1 py-2">
            {isMcq ? (
              <div className="space-y-3.5">
                {[
                  { key: "option1", text: currentQuestion?.option1 },
                  { key: "option2", text: currentQuestion?.option2 },
                  { key: "option3", text: currentQuestion?.option3 },
                  { key: "option4", text: currentQuestion?.option4 },
                ]
                  .filter((opt) => opt.text && opt.text.trim() !== "")
                  .map((opt, idx) => {
                    const isSelected = mcqAnswers[currentQuestionId] === opt.text;
                    return (
                      <button
                        key={opt.key}
                        type="button"
                        disabled={isAttemptExpired}
                        onClick={() => handleSelectOption(currentQuestion, opt.key, opt.text)}
                        className={`w-full text-left p-4 sm:p-5 rounded-2xl border transition-all flex items-start gap-4 ${
                          isSelected
                            ? "bg-orange-500/20 border-orange-500 text-white shadow-md ring-1 ring-orange-500/50"
                            : "bg-slate-800/40 border-slate-700/80 text-slate-200 hover:bg-slate-800 hover:border-slate-600"
                        }`}
                      >
                        <div
                          className={`w-7 h-7 rounded-xl font-bold text-xs flex items-center justify-center shrink-0 mt-0.5 border ${
                            isSelected
                              ? "bg-orange-500 border-orange-400 text-white"
                              : "bg-slate-800 border-slate-700 text-slate-400"
                          }`}
                        >
                          {String.fromCharCode(65 + idx)}
                        </div>
                        <span className="text-sm sm:text-base font-medium leading-relaxed">{opt.text}</span>
                      </button>
                    );
                  })}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs text-slate-400 font-semibold">
                  <span>Write your answer:</span>
                  <span>Word Limit: ~{currentQuestion?.word_limit || currentQuestion?.wordLimit || 250} words</span>
                </div>
                <textarea
                  rows={8}
                  value={textAnswers[currentQuestionId] || ""}
                  disabled={isAttemptExpired}
                  onChange={(e) => handleTextAnswerChange(currentQuestionId, e.target.value)}
                  placeholder="Type your answer..."
                  className="w-full bg-slate-950/80 border border-slate-700 rounded-2xl p-4 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 transition-all resize-y"
                />
              </div>
            )}
          </div>

          <div className="flex items-center justify-between pt-6 border-t border-slate-800">
            <button
              onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
              disabled={currentIndex === 0}
              className="py-2.5 px-5 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-bold rounded-xl transition-all"
            >
              ← Previous
            </button>

            <span className="text-xs text-slate-400 font-bold hidden sm:inline">
              Answered: {answeredCount} / {questions.length}
            </span>

            {currentIndex < questions.length - 1 ? (
              <button
                onClick={() => setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))}
                className="py-2.5 px-6 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white text-xs sm:text-sm font-bold rounded-xl shadow transition-all"
              >
                Next Question →
              </button>
            ) : (
              <button
                onClick={() => setShowSubmitModal(true)}
                className="py-2.5 px-6 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs sm:text-sm font-black rounded-xl shadow-lg transition-all"
              >
                Submit Exam ✓
              </button>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-6 backdrop-blur-sm space-y-5">
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-400">
              Question Navigator ({questions.length})
            </h3>

            <div className="grid grid-cols-5 gap-2.5 max-h-60 overflow-y-auto pr-1">
              {questions.map((q, idx) => {
                const qId = q.question_id || q.questionId || idx + 1;
                const isAnswered = isMcq ? !!mcqAnswers[qId] : !!textAnswers[qId];
                const isFlagged = flagged.has(qId);
                const isCurrent = currentIndex === idx;

                let colorClasses = "bg-slate-800 text-slate-400 border-slate-700";
                if (isCurrent) {
                  colorClasses = "bg-orange-500 text-white border-orange-400 ring-2 ring-orange-500/40 font-black";
                } else if (isFlagged) {
                  colorClasses = "bg-amber-500/20 text-amber-300 border-amber-500 font-bold";
                } else if (isAnswered) {
                  colorClasses = "bg-emerald-500/20 text-emerald-300 border-emerald-500/50 font-bold";
                }

                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setCurrentIndex(idx)}
                    className={`h-10 rounded-xl text-xs font-bold border transition-all flex items-center justify-center ${colorClasses}`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>

            <div className="pt-4 border-t border-slate-800 grid grid-cols-2 gap-2 text-[11px] text-slate-400 font-medium">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
                <span>Answered ({answeredCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-slate-700"></span>
                <span>Remaining ({questions.length - answeredCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
                <span>Flagged ({flagged.size})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
                <span>Current</span>
              </div>
            </div>
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-6 backdrop-blur-sm text-xs text-slate-400 space-y-2">
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-400">Before you submit</h3>
            <ul className="space-y-1.5 list-disc list-inside">
              <li>Every answer you give is saved on the server as you type.</li>
              <li>The countdown follows the attempt window set by the server.</li>
              <li>You can review and change answers until you submit or time runs out.</li>
            </ul>
          </div>
        </div>
      </main>

      {showSubmitModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl space-y-6 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 mx-auto flex items-center justify-center">
              <Icon name="check-circle" className="w-8 h-8" />
            </div>

            <div>
              <h3 className="text-xl font-black text-white">Submit Exam?</h3>
              <p className="text-sm text-slate-300 mt-2 leading-relaxed">
                You have answered <strong className="text-white font-bold">{answeredCount}</strong> of{" "}
                <strong className="text-white font-bold">{questions.length}</strong> questions. You cannot change your answers after submitting.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowSubmitModal(false)}
                disabled={isSubmitting}
                className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl text-sm transition-all"
              >
                Continue Exam
              </button>
              <button
                type="button"
                onClick={() => submitExam()}
                disabled={isSubmitting}
                className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-bold rounded-xl text-sm transition-all shadow-lg flex items-center justify-center gap-2"
              >
                {isSubmitting ? "Submitting..." : "Yes, Submit"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
