import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { getUpcomingExams, getStudentRegistrations, registerForExam } from "../../api/examApi";
import { getStudentResults } from "../../api/resultApi";
import { parseExamSchedule } from "../../utils/dateUtils";
import { QUESTION_TYPES } from "../../constants/examConstants";

export default function AvailableExams() {
  const [exams, setExams] = useState([]);
  const [submittedExamsMap, setSubmittedExamsMap] = useState({});
  const [registeredExamsMap, setRegisteredExamsMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [registeringId, setRegisteringId] = useState(null);
  const [search, setSearch] = useState("");
  const [filterType, setFilterType] = useState("ALL"); // ALL, REGISTERED, MCQ, QUESTION_ANSWER
  const [currentTime, setCurrentTime] = useState(new Date());

  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  // Live ticker for real-time window status and countdowns
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 5000);
    return () => clearInterval(timer);
  }, []);

  const fetchUpcomingExamsAndRegistrations = useCallback(async () => {
    setLoading(true);
    try {
      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;

      // 1. Fetch upcoming exams
      const fetchedExams = await getUpcomingExams();
      setExams(fetchedExams);

      // 2. Fetch student's registrations
      try {
        const regIds = await getStudentRegistrations(studentId);
        const regMap = {};
        regIds.forEach((id) => {
          if (typeof id === "number" || typeof id === "string") {
            regMap[id] = true;
          } else if (id && (id.examId || id.exam_id)) {
            regMap[id.examId || id.exam_id] = true;
          }
        });
        setRegisteredExamsMap(regMap);
      } catch (regErr) {
        console.warn("Could not fetch registrations from server:", regErr);
        try {
          const localRegs = JSON.parse(localStorage.getItem(`student_${studentId}_registered_exams`) || "{}");
          setRegisteredExamsMap(localRegs);
        } catch {
          // ignore fallback error
        }
      }

      // 3. Fetch student's completed results to enforce single attempt policy
      try {
        const results = await getStudentResults(studentId);
        const map = {};
        results.forEach((r) => {
          const eId = r.exam_id || r.examId;
          if (eId) {
            map[eId] = r;
          }
        });
        setSubmittedExamsMap(map);
      } catch (err) {
        showError(err.message || "Failed to load examination results");
        setSubmittedExamsMap({});
      }
    } catch (err) {
      showError(err.message || "Failed to fetch upcoming exams");
    } finally {
      setLoading(false);
    }
  }, [user?.roleId, showError]);

  useEffect(() => {
    fetchUpcomingExamsAndRegistrations();
  }, [fetchUpcomingExamsAndRegistrations]);

  const handleRegister = async (examId, examName) => {
    const studentId = user?.roleId || localStorage.getItem("student_id") || 1;
    setRegisteringId(examId);

    try {
      const res = await registerForExam(examId, studentId);

      showSuccess(res?.message || `Successfully registered for ${examName}!`);
      setRegisteredExamsMap((prev) => {
        const updated = { ...prev, [examId]: true };
        try {
          localStorage.setItem(`student_${studentId}_registered_exams`, JSON.stringify(updated));
        } catch {
          // ignore storage error
        }
        return updated;
      });
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to register for examination";
      showError(errorMsg);
    } finally {
      setRegisteringId(null);
    }
  };

  const getExamTimingInfo = (exam) => parseExamSchedule(exam, currentTime);

  const filteredExams = exams.filter((e) => {
    const examId = e.exam_id || e.examId;
    const title = (e.exam_name || e.examName || "").toLowerCase();
    const subName = (e.subject?.subject_name || e.subject?.subjectName || "").toLowerCase();
    const matchesSearch = title.includes(search.toLowerCase()) || subName.includes(search.toLowerCase());

    const type = (e.exam_type || e.examType || QUESTION_TYPES.MCQ).toUpperCase();
    const isRegistered = !!registeredExamsMap[examId];

    if (!matchesSearch) return false;

    if (filterType === "ALL") return true;
    if (filterType === "REGISTERED") return isRegistered;
    if (filterType === QUESTION_TYPES.MCQ) return type === QUESTION_TYPES.MCQ;
    if (filterType === QUESTION_TYPES.QUESTION_ANSWER) return type === QUESTION_TYPES.QUESTION_ANSWER;

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="book" className="w-5 h-5 text-orange-500" />
          Examination Portal
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Register for scheduled assessments. Only registered students with valid active time windows can attend.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative flex-1 w-full">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by exam name or subject..."
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white transition-all"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto">
          {[
            { id: "ALL", label: "All Exams" },
            { id: "REGISTERED", label: `My Registered (${Object.keys(registeredExamsMap).length})` },
            { id: "MCQ", label: "MCQ Format" },
            { id: "QUESTION_ANSWER", label: "Q&A (Theory)" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterType(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                filterType === tab.id
                  ? "bg-slate-900 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Exam Cards Grid */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading examinations and registration status...</p>
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="book" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">
            {filterType === "REGISTERED" ? "No Registered Examinations Found" : "No examinations currently available"}
          </h3>
          <p className="text-xs text-slate-400">
            {filterType === "REGISTERED"
              ? "Browse 'All Exams' to register for upcoming tests."
              : "Check back later or contact your instructor for scheduled tests."}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredExams.map((exam) => {
            const examId = exam.exam_id || exam.examId;
            const examType = (exam.exam_type || exam.examType || "MCQ").toUpperCase();
            const isMcq = examType === "MCQ";
            const subjectName = exam.subject?.subject_name || exam.subject?.subjectName || "General";
            const duration = exam.duration_minutes || exam.durationMinutes || 60;
            const totalMarks = exam.total_marks || exam.totalMarks || 100;
            const passMarks = exam.passing_marks || exam.passingMarks || 40;

            const existingResult = submittedExamsMap[examId];
            const isSubmitted = !!existingResult;
            const isRegistered = !!registeredExamsMap[examId];

            const {
              datePart,
              startTimeStr,
              endTimeStr,
              isRegistrationClosed,
              isExamUpcoming,
              isExamActive,
            } = getExamTimingInfo(exam);

            const isRegisteringThis = registeringId === examId;

            return (
              <div
                key={examId}
                className={`bg-white rounded-2xl border shadow-sm hover:shadow-md transition-all p-5 flex flex-col justify-between space-y-4 ${
                  isSubmitted
                    ? "border-emerald-300 bg-emerald-50/20"
                    : isRegistered && isExamActive
                    ? "border-orange-400 ring-2 ring-orange-200/60"
                    : isRegistered
                    ? "border-emerald-200"
                    : "border-slate-200"
                }`}
              >
                {/* Card Header */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <span className="text-xs font-bold text-slate-600 bg-slate-100 px-2.5 py-1 rounded-md">
                      {subjectName}
                    </span>

                    <div className="flex items-center gap-1.5 flex-wrap">
                      {/* Submission Status */}
                      {isSubmitted ? (
                        <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1">
                          <Icon name="check-circle" className="w-3.5 h-3.5" />
                          Submitted
                        </span>
                      ) : isRegistered ? (
                        <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1">
                          <Icon name="check-circle" className="w-3.5 h-3.5" />
                          Registered
                        </span>
                      ) : isRegistrationClosed ? (
                        <span className="text-xs font-bold px-2.5 py-1 rounded-md bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-1">
                          <Icon name="lock" className="w-3.5 h-3.5 text-rose-500" />
                          <span>Registration Closed</span>
                        </span>
                      ) : (
                        <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-indigo-100 text-indigo-800 border border-indigo-200">
                          Registration Open
                        </span>
                      )}

                      {/* Format Badge */}
                      <span
                        className={`text-xs font-bold px-2.5 py-1 rounded-md ${
                          isMcq ? "bg-orange-100 text-orange-700" : "bg-blue-100 text-blue-700"
                        }`}
                      >
                        {isMcq ? "MCQ" : "Theory"}
                      </span>
                    </div>
                  </div>

                  <h3 className="font-bold text-slate-900 text-base leading-snug">
                    {exam.exam_name || exam.examName}
                  </h3>
                </div>

                {/* Exam Specs */}
                <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="flex items-center gap-1.5">
                    <Icon name="clock" className="w-3.5 h-3.5 text-slate-400" />
                    <span>Duration: <strong className="text-slate-800">{duration} mins</strong></span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Icon name="award" className="w-3.5 h-3.5 text-slate-400" />
                    <span>Passing: <strong className="text-slate-800">{passMarks}/{totalMarks}</strong></span>
                  </div>
                  <div className="flex items-center gap-1.5 col-span-2">
                    <Icon name="clock" className="w-3.5 h-3.5 text-slate-400" />
                    <span>Slot: <strong className="text-slate-800 font-mono">{datePart} ({startTimeStr} - {endTimeStr})</strong></span>
                  </div>
                </div>

                {/* Card CTA Actions */}
                <div className="pt-1">
                  {isSubmitted ? (
                    /* 1. Already Submitted -> View Scorecard */
                    <button
                      type="button"
                      onClick={() =>
                        navigate(`/take-exam/${examId}`, {
                          state: {
                            examId,
                            examName: exam.exam_name || exam.examName,
                            examType: (exam.exam_type || exam.examType || "MCQ").toUpperCase(),
                            subjectName,
                          },
                        })
                      }
                      className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-semibold rounded-xl text-sm shadow-sm transition-all flex items-center justify-center gap-2"
                    >
                      <Icon name="award" className="w-4 h-4" />
                      <span>View Final Scorecard</span>
                    </button>
                  ) : isRegistered ? (
                    /* 2. Registered Student Options */
                    isExamActive ? (
                      /* 2A. Registered & Timing is Active NOW -> Can enter exam */
                      <button
                        type="button"
                        onClick={() =>
                          navigate(`/take-exam/${examId}`, {
                            state: {
                              examId,
                              examName: exam.exam_name || exam.examName,
                              examType: (exam.exam_type || exam.examType || "MCQ").toUpperCase(),
                              subjectName,
                              durationMinutes: duration,
                              totalMarks,
                              passingMarks: passMarks,
                            },
                          })
                        }
                        className="w-full py-2.5 px-4 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-bold rounded-xl text-sm shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 animate-pulse"
                      >
                        <Icon name="check-circle" className="w-4 h-4" />
                        <span>Attend Examination Hall →</span>
                      </button>
                    ) : isExamUpcoming ? (
                      /* 2B. Registered & Timing is in Future -> Disabled with countdown */
                      <button
                        type="button"
                        disabled
                        className="w-full py-2.5 px-4 bg-blue-50 border border-blue-200 text-blue-700 font-semibold rounded-xl text-sm cursor-not-allowed flex items-center justify-center gap-2"
                        title={`Exam starts on ${datePart} at ${startTimeStr}.`}
                      >
                        <Icon name="clock" className="w-4 h-4 text-blue-500" />
                        <span>Exam Not Started Yet</span>
                      </button>
                    ) : (
                      /* 2C. Registered & Timing Expired -> Disabled */
                      <button
                        type="button"
                        disabled
                        className="w-full py-2.5 px-4 bg-slate-100 border border-slate-200 text-slate-500 font-semibold rounded-xl text-sm cursor-not-allowed flex items-center justify-center gap-2 select-none"
                        title="The scheduled time window for this exam has passed."
                      >
                        <Icon name="clock" className="w-4 h-4 text-slate-400" />
                        <span>Exam Window Expired</span>
                      </button>
                    )
                  ) : (
                    /* 3. Not Registered Student */
                    !isRegistrationClosed ? (
                      /* 3A. Not Registered & Registration Open -> Can Register */
                      <button
                        type="button"
                        disabled={isRegisteringThis}
                        onClick={() => handleRegister(examId, exam.exam_name || exam.examName)}
                        className="w-full py-2.5 px-4 bg-slate-900 hover:bg-slate-800 active:bg-slate-950 text-white font-semibold rounded-xl text-sm shadow-sm transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                      >
                        {isRegisteringThis ? (
                          <>
                            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                            <span>Registering...</span>
                          </>
                        ) : (
                          <>
                            <Icon name="plus" className="w-4 h-4" />
                            <span>Register for Exam</span>
                          </>
                        )}
                      </button>
                    ) : (
                      /* 3B. Not Registered & Registration Deadline Passed */
                      <button
                        type="button"
                        disabled
                        className="w-full py-2.5 px-4 bg-gradient-to-r from-rose-50/90 via-rose-50/60 to-slate-50 border border-rose-200 text-rose-700 font-semibold rounded-xl text-sm cursor-not-allowed flex items-center justify-center gap-2 shadow-xs select-none transition-all"
                        title="Registration deadline for this examination has passed."
                      >
                        <span className="w-5 h-5 rounded-lg bg-rose-100 text-rose-600 flex items-center justify-center flex-shrink-0 shadow-2xs">
                          <Icon name="lock" className="w-3.5 h-3.5" />
                        </span>
                        <span className="font-bold">Registration Closed</span>
                      </button>
                    )
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
