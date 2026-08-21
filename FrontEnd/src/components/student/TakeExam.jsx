import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import apiClient from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function TakeExam() {
  const { examId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showSuccess, showError, showWarning } = useToast();

  const [exam, setExam] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [mcqAnswers, setMcqAnswers] = useState({}); // { questionId: selectedOptionText }
  const [textAnswers, setTextAnswers] = useState({}); // { questionId: writtenText }
  const [flagged, setFlagged] = useState(new Set()); // Set of questionIds

  const [timeLeftSeconds, setTimeLeftSeconds] = useState(3600);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [resultData, setResultData] = useState(null);

  const isMcq = (exam?.exam_type || exam?.examType || questions[currentIndex]?.questionType || "MCQ").toUpperCase() === "MCQ";

  const fetchExamAndQuestions = useCallback(async () => {
    setLoading(true);
    try {
      // 1. Fetch Exam details
      let examDetails = location.state;
      if (!examDetails?.examName) {
        try {
          const res = await apiClient.get(`/api/exams/${examId}`);
          examDetails = res.data;
        } catch {
          console.warn("Using default exam structure");
        }
      }
      setExam(examDetails || { exam_name: "Examination", exam_type: "MCQ" });

      const initialDuration = (examDetails?.duration_minutes || examDetails?.durationMinutes || 60) * 60;
      setTimeLeftSeconds(initialDuration);

      // 2. Fetch Questions for this exam
      const qRes = await apiClient.get(`/api/exams/${examId}/questions`);
      let fetchedQuestions = Array.isArray(qRes.data) ? qRes.data : [];

      // Fallback sample questions if teacher hasn't added any yet
      if (fetchedQuestions.length === 0) {
        const isMcqExam = (examDetails?.exam_type || examDetails?.examType || "MCQ") === "MCQ";
        if (isMcqExam) {
          fetchedQuestions = [
            {
              questionId: 101,
              question_id: 101,
              question: "What is the primary function of an operating system kernel?",
              option1: "Manage hardware resources and provide essential services to applications",
              option2: "Compile high-level programming language code into machine binaries",
              option3: "Provide a web browsing interface for end users",
              option4: "Design user interface graphics and typography",
              correctOption: "Manage hardware resources and provide essential services to applications",
              questionType: "MCQ",
            },
            {
              questionId: 102,
              question_id: 102,
              question: "Which of the following data structures operates on a Last-In, First-Out (LIFO) principle?",
              option1: "Queue",
              option2: "Stack",
              option3: "Linked List",
              option4: "Binary Search Tree",
              correctOption: "Stack",
              questionType: "MCQ",
            },
            {
              questionId: 103,
              question_id: 103,
              question: "What is the time complexity of searching an element in a balanced Binary Search Tree?",
              option1: "O(1)",
              option2: "O(n)",
              option3: "O(log n)",
              option4: "O(n log n)",
              correctOption: "O(log n)",
              questionType: "MCQ",
            },
          ];
        } else {
          fetchedQuestions = [
            {
              questionId: 201,
              question_id: 201,
              question: "Explain the concept of Polymorphism in Object-Oriented Programming with real-world examples.",
              sampleAnswer: "Polymorphism allows objects of different classes to be treated as objects of a common superclass.",
              marks: 10,
              wordLimit: 250,
              questionType: "QUESTION_ANSWER",
            },
            {
              questionId: 202,
              question_id: 202,
              question: "Describe ACID properties in database management systems and why they are essential.",
              sampleAnswer: "Atomicity, Consistency, Isolation, Durability ensure database transaction reliability.",
              marks: 10,
              wordLimit: 200,
              questionType: "QUESTION_ANSWER",
            },
          ];
        }
      }

      setQuestions(fetchedQuestions);

      // Restore saved local draft if any
      const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
      const savedDraft = localStorage.getItem(draftKey);
      if (savedDraft) {
        try {
          const parsed = JSON.parse(savedDraft);
          if (parsed.mcqAnswers) setMcqAnswers(parsed.mcqAnswers);
          if (parsed.textAnswers) setTextAnswers(parsed.textAnswers);
        } catch {
          console.warn("Could not parse draft");
        }
      }
    } catch (err) {
      showError(err.message || "Failed to load examination data");
    } finally {
      setLoading(false);
    }
  }, [examId, location.state, user?.userId, showError]);

  useEffect(() => {
    fetchExamAndQuestions();
  }, [fetchExamAndQuestions]);

  const submitFinalExam = useCallback(async () => {
    setIsSubmitting(true);
    setShowSubmitModal(false);

    try {
      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;
      const studentName = user?.username || "Student";

      const submissionPayload = {
        exam_id: parseInt(examId, 10),
        examId: parseInt(examId, 10),
        student_id: parseInt(studentId, 10),
        studentId: parseInt(studentId, 10),
        student_name: studentName,
        exam_type: isMcq ? "MCQ" : "QUESTION_ANSWER",
        mcq_answers: mcqAnswers,
        text_answers: textAnswers,
      };

      const res = await apiClient.post("/api/results/submit", submissionPayload);
      const result = res.data;

      // Clean local draft
      const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
      localStorage.removeItem(draftKey);

      setResultData(result);
      showSuccess("Exam submitted successfully!");
    } catch (err) {
      showError(err.message || "Failed to submit exam");
    } finally {
      setIsSubmitting(false);
    }
  }, [examId, isMcq, mcqAnswers, textAnswers, user?.roleId, user?.username, user?.userId, showError, showSuccess]);

  const handleAutoSubmit = useCallback(() => {
    showWarning("Time expired! Automatically submitting your examination.");
    submitFinalExam();
  }, [showWarning, submitFinalExam]);

  // Timer countdown
  useEffect(() => {
    if (resultData || loading) return;

    const timer = setInterval(() => {
      setTimeLeftSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [resultData, loading, handleAutoSubmit]);

  // Autosave draft
  useEffect(() => {
    if (questions.length > 0 && !resultData) {
      const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
      localStorage.setItem(
        draftKey,
        JSON.stringify({ mcqAnswers, textAnswers, timestamp: Date.now() })
      );
    }
  }, [mcqAnswers, textAnswers, examId, user, questions, resultData]);

  const formatTimer = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  const currentQ = questions[currentIndex];
  const currentQId = currentQ?.question_id || currentQ?.questionId || currentIndex + 1;

  const handleSelectOption = (optionText) => {
    setMcqAnswers((prev) => ({
      ...prev,
      [currentQId]: optionText,
    }));
  };

  const handleTextAnswerChange = (val) => {
    setTextAnswers((prev) => ({
      ...prev,
      [currentQId]: val,
    }));
  };

  const toggleFlag = () => {
    setFlagged((prev) => {
      const next = new Set(prev);
      if (next.has(currentQId)) {
        next.delete(currentQId);
      } else {
        next.add(currentQId);
      }
      return next;
    });
  };

  const answeredCount = useMemo(() => {
    if (isMcq) {
      return Object.keys(mcqAnswers).filter((k) => mcqAnswers[k] && mcqAnswers[k].trim() !== "").length;
    } else {
      return Object.keys(textAnswers).filter((k) => textAnswers[k] && textAnswers[k].trim() !== "").length;
    }
  }, [isMcq, mcqAnswers, textAnswers]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
        <div className="w-10 h-10 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mb-3"></div>
        <p className="text-sm font-medium text-slate-300">Initializing Exam Session...</p>
      </div>
    );
  }

  // POST-SUBMISSION RESULT VIEW
  if (resultData) {
    const isPass = resultData.status === "Pass";
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-4 sm:p-6">
        <div className="w-full max-w-2xl bg-white text-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
          {/* Result Card Header */}
          <div className={`p-8 text-center ${isPass ? "bg-emerald-500 text-white" : "bg-slate-900 text-white"}`}>
            <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-3">
              <Icon name={isPass ? "check-circle" : "award"} className="w-8 h-8 text-white" />
            </div>
            <h2 className="text-2xl font-black">{isPass ? "Assessment Completed!" : "Exam Submitted"}</h2>
            <p className="text-xs text-white/80 mt-1">
              {resultData.examName || exam?.exam_name} • {resultData.subjectName}
            </p>
          </div>

          {/* Scores Overview */}
          <div className="p-6 sm:p-8 space-y-6">
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Score</span>
                <p className="text-2xl font-extrabold text-slate-900 mt-1">
                  {resultData.marksObtained} <span className="text-xs font-normal text-slate-400">/ {resultData.totalMarks}</span>
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Percentage</span>
                <p className="text-2xl font-extrabold text-orange-600 mt-1">
                  {resultData.percentage}%
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Outcome</span>
                <p className={`text-base font-bold mt-2 ${isPass ? "text-emerald-600" : "text-rose-600"}`}>
                  {resultData.status}
                </p>
              </div>
            </div>

            {/* MCQ Breakdown */}
            {resultData.questionBreakdown && resultData.questionBreakdown.length > 0 && (
              <div className="space-y-3 pt-2">
                <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Question Review ({resultData.questionBreakdown.length})
                </h4>
                <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
                  {resultData.questionBreakdown.map((item, idx) => (
                    <div
                      key={idx}
                      className={`p-3 rounded-xl border text-xs space-y-1 ${
                        item.isCorrect ? "bg-emerald-50/70 border-emerald-200" : "bg-rose-50/70 border-rose-200"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-semibold text-slate-800">
                          {idx + 1}. {item.question}
                        </p>
                        <span
                          className={`font-bold px-2 py-0.5 rounded text-[10px] ${
                            item.isCorrect ? "bg-emerald-200 text-emerald-800" : "bg-rose-200 text-rose-800"
                          }`}
                        >
                          {item.isCorrect ? "Correct (+1)" : "Incorrect (0)"}
                        </span>
                      </div>
                      <p className="text-slate-600">
                        Your answer: <strong className="text-slate-900">{item.selectedAnswer || "None"}</strong>
                      </p>
                      {!item.isCorrect && item.correctAnswer && (
                        <p className="text-emerald-700 font-medium">
                          Correct answer: {item.correctAnswer}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-slate-100">
              <button
                onClick={() => navigate("/dashboard/student")}
                className="flex-1 py-3 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl text-sm transition-all text-center"
              >
                Return to Dashboard
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ACTIVE EXAM TAKING INTERFACE
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Bar Header */}
      <header className="h-16 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              if (window.confirm("Are you sure you want to leave the exam session? Your draft answers are saved.")) {
                navigate("/dashboard/student");
              }
            }}
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors text-xs flex items-center gap-1"
          >
            ← Exit
          </button>
          <div>
            <h1 className="text-sm font-bold text-white leading-none truncate max-w-xs sm:max-w-md">
              {exam?.exam_name || exam?.examName}
            </h1>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {exam?.subject?.subject_name || "General Subject"} • {isMcq ? "MCQ Format" : "Descriptive Q&A"}
            </p>
          </div>
        </div>

        {/* Live Timer & Submit CTA */}
        <div className="flex items-center gap-4">
          <div
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-mono font-bold border transition-colors ${
              timeLeftSeconds < 300
                ? "bg-rose-950/80 border-rose-500 text-rose-400 animate-pulse"
                : "bg-slate-800 border-slate-700 text-orange-400"
            }`}
          >
            <Icon name="clock" className="w-3.5 h-3.5" />
            <span>{formatTimer(timeLeftSeconds)}</span>
          </div>

          <button
            onClick={() => setShowSubmitModal(true)}
            className="py-2 px-4 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow transition-all"
          >
            Finish & Submit
          </button>
        </div>
      </header>

      {/* Main Examination Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Question Panel (3 Columns) */}
        <div className="lg:col-span-3 flex flex-col justify-between bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 sm:p-8 backdrop-blur-sm space-y-6">
          {/* Question Header */}
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <span className="text-xs font-black px-2.5 py-1 rounded-lg bg-orange-500 text-white">
                  Question {currentIndex + 1}
                </span>
                <span className="text-xs text-slate-400">of {questions.length}</span>
              </div>

              <button
                type="button"
                onClick={toggleFlag}
                className={`text-xs font-semibold px-3 py-1 rounded-lg border transition-all flex items-center gap-1.5 ${
                  flagged.has(currentQId)
                    ? "bg-amber-500/20 border-amber-500 text-amber-300"
                    : "bg-slate-800/60 border-slate-700 text-slate-400 hover:text-white"
                }`}
              >
                ★ {flagged.has(currentQId) ? "Flagged for Review" : "Flag for Review"}
              </button>
            </div>

            {/* Question Text */}
            <h2 className="text-base sm:text-lg font-semibold text-slate-100 leading-relaxed">
              {currentQ?.question}
            </h2>
          </div>

          {/* Options / Answer Input Area */}
          <div className="flex-1 py-2">
            {isMcq ? (
              <div className="space-y-3">
                {[
                  { key: "option1", text: currentQ?.option1 },
                  { key: "option2", text: currentQ?.option2 },
                  { key: "option3", text: currentQ?.option3 },
                  { key: "option4", text: currentQ?.option4 },
                ]
                  .filter((opt) => opt.text && opt.text.trim() !== "")
                  .map((opt, oIdx) => {
                    const isSelected = mcqAnswers[currentQId] === opt.text;
                    const letter = String.fromCharCode(65 + oIdx);

                    return (
                      <div
                        key={oIdx}
                        onClick={() => handleSelectOption(opt.text)}
                        className={`flex items-center gap-4 p-4 rounded-2xl border-2 cursor-pointer transition-all ${
                          isSelected
                            ? "bg-orange-500/10 border-orange-500 text-orange-200 ring-2 ring-orange-500/20 shadow-md"
                            : "bg-slate-800/40 border-slate-800 text-slate-300 hover:bg-slate-800/80 hover:border-slate-700"
                        }`}
                      >
                        <div
                          className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-bold transition-colors ${
                            isSelected ? "bg-orange-500 text-white" : "bg-slate-700/60 text-slate-400"
                          }`}
                        >
                          {letter}
                        </div>
                        <span className="flex-1 text-sm font-medium leading-relaxed">{opt.text}</span>
                        <div
                          className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                            isSelected ? "border-orange-500 bg-orange-500" : "border-slate-600"
                          }`}
                        >
                          {isSelected && <div className="w-1.5 h-1.5 bg-white rounded-full"></div>}
                        </div>
                      </div>
                    );
                  })}
              </div>
            ) : (
              /* Descriptive Answer Textarea */
              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs text-slate-400">
                  <span>Type your detailed answer below:</span>
                  <span>Word Count: {(textAnswers[currentQId] || "").trim().split(/\s+/).filter(Boolean).length} words</span>
                </div>
                <textarea
                  rows="9"
                  value={textAnswers[currentQId] || ""}
                  onChange={(e) => handleTextAnswerChange(e.target.value)}
                  placeholder="Write your explanation or theoretical response here..."
                  className="w-full p-4 bg-slate-950/80 border border-slate-800 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 rounded-2xl text-sm text-slate-200 outline-none leading-relaxed resize-none transition-all"
                />
              </div>
            )}
          </div>

          {/* Navigation Controls */}
          <div className="flex items-center justify-between pt-4 border-t border-slate-800">
            <button
              type="button"
              disabled={currentIndex === 0}
              onClick={() => setCurrentIndex((prev) => prev - 1)}
              className="py-2.5 px-5 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:hover:bg-slate-800 text-slate-300 text-xs font-semibold rounded-xl transition-all"
            >
              ← Previous
            </button>

            <div className="flex items-center gap-2">
              {currentIndex < questions.length - 1 ? (
                <button
                  type="button"
                  onClick={() => setCurrentIndex((prev) => prev + 1)}
                  className="py-2.5 px-6 bg-orange-500 hover:bg-orange-600 text-white text-xs font-bold rounded-xl shadow transition-all"
                >
                  Next Question →
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setShowSubmitModal(true)}
                  className="py-2.5 px-6 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow transition-all"
                >
                  Review & Submit ✓
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar Question Palette (1 Column) */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-5 backdrop-blur-sm flex flex-col space-y-5">
          <div>
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Question Palette
            </h3>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Answered: <strong className="text-emerald-400">{answeredCount}</strong> / {questions.length}
            </p>
          </div>

          {/* Palette Grid */}
          <div className="grid grid-cols-4 sm:grid-cols-5 gap-2">
            {questions.map((q, idx) => {
              const qId = q.question_id || q.questionId || idx + 1;
              const isAnswered = isMcq ? !!mcqAnswers[qId] : !!textAnswers[qId]?.trim();
              const isCurrent = idx === currentIndex;
              const isFlag = flagged.has(qId);

              let btnClass = "bg-slate-800 text-slate-400 border-slate-700";
              if (isCurrent) {
                btnClass = "ring-2 ring-orange-500 text-white font-bold";
              }
              if (isAnswered) {
                btnClass += " bg-emerald-950/70 border-emerald-500 text-emerald-300";
              }
              if (isFlag) {
                btnClass += " bg-amber-950/70 border-amber-500 text-amber-300";
              }

              return (
                <button
                  key={idx}
                  onClick={() => setCurrentIndex(idx)}
                  className={`h-10 rounded-xl border text-xs font-semibold flex items-center justify-center relative transition-all hover:scale-105 ${btnClass}`}
                >
                  {idx + 1}
                  {isFlag && <span className="absolute -top-1 -right-1 text-[9px] text-amber-400">★</span>}
                </button>
              );
            })}
          </div>

          {/* Legend */}
          <div className="pt-3 border-t border-slate-800 space-y-2 text-[11px] text-slate-400">
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-md bg-emerald-900 border border-emerald-500"></div>
              <span>Answered</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-md bg-amber-900 border border-amber-500"></div>
              <span>Flagged for Review</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-md bg-slate-800 border border-slate-700"></div>
              <span>Not Answered</span>
            </div>
          </div>
        </div>
      </main>

      {/* Confirmation Submit Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-5">
            <div className="text-center space-y-2">
              <div className="w-12 h-12 rounded-full bg-orange-500/20 text-orange-400 flex items-center justify-center mx-auto">
                <Icon name="check-circle" className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white">Ready to submit your exam?</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                You have answered <strong className="text-white">{answeredCount}</strong> out of{" "}
                <strong className="text-white">{questions.length}</strong> questions.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowSubmitModal(false)}
                className="flex-1 py-2.5 px-4 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-xl transition-colors"
              >
                Continue Exam
              </button>
              <button
                type="button"
                disabled={isSubmitting}
                onClick={submitFinalExam}
                className="flex-1 py-2.5 px-4 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow transition-all"
              >
                {isSubmitting ? "Submitting..." : "Confirm & Submit"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
