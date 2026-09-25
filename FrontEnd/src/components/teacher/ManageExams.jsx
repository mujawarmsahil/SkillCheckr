import React, { useState, useEffect, useCallback } from "react";
import { getAllExams, getTeacherExams, getExamQuestions, deleteExam } from "../../api/examApi";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function ManageExams({ onAddNew }) {
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();

  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterType, setFilterType] = useState("ALL"); // ALL, MCQ, QUESTION_ANSWER

  const [selectedExam, setSelectedExam] = useState(null);
  const [examQuestions, setExamQuestions] = useState([]);
  const [loadingQuestions, setLoadingQuestions] = useState(false);

  const fetchExams = useCallback(async () => {
    setLoading(true);
    try {
      const teacherId = user?.roleId || localStorage.getItem("teacher_id");
      setExams(teacherId ? await getTeacherExams(teacherId) : await getAllExams());
    } catch (err) {
      if (err.status === 404) {
        setExams([]);
      } else {
        showError(err.message || "Failed to load exams");
      }
    } finally {
      setLoading(false);
    }
  }, [user?.roleId, showError]);

  useEffect(() => {
    fetchExams();
  }, [fetchExams]);

  const handleDelete = async (examId) => {
    if (!window.confirm("Are you sure you want to delete this exam? All associated questions will be removed.")) {
      return;
    }

    try {
      await deleteExam(examId);
      showSuccess("Exam deleted successfully");
      setExams((prev) => prev.filter((e) => e.exam_id !== examId && e.examId !== examId));
    } catch (err) {
      showError(err.message || "Failed to delete exam");
    }
  };

  const handleViewQuestions = async (exam) => {
    const examId = exam.exam_id || exam.examId;
    setSelectedExam(exam);
    setLoadingQuestions(true);
    try {
      setExamQuestions(await getExamQuestions(examId));
    } catch {
      showError("Could not load questions for this exam");
      setExamQuestions([]);
    } finally {
      setLoadingQuestions(false);
    }
  };

  const filteredExams = exams.filter((e) => {
    const title = (e.exam_name || e.examName || "").toLowerCase();
    const subName = (e.subject?.subject_name || e.subject?.subjectName || "").toLowerCase();
    const matchesSearch = title.includes(search.toLowerCase()) || subName.includes(search.toLowerCase());

    const type = (e.exam_type || e.examType || "MCQ").toUpperCase();
    const matchesType = filterType === "ALL" || type === filterType;

    return matchesSearch && matchesType;
  });

  return (
    <div className="space-y-6">
      {/* Top Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Icon name="file-text" className="w-5 h-5 text-orange-500" />
            Manage Created Examinations
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Total configured exams: <span className="font-semibold text-slate-800">{exams.length}</span>
          </p>
        </div>

        {onAddNew && (
          <button
            onClick={onAddNew}
            className="py-2.5 px-4 bg-orange-500 hover:bg-orange-600 text-white text-sm font-semibold rounded-xl shadow-sm transition-all flex items-center gap-2"
          >
            <Icon name="plus" className="w-4 h-4" />
            Create New Exam
          </button>
        )}
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
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto">
          {[
            { id: "ALL", label: "All Formats" },
            { id: "MCQ", label: "MCQ Only" },
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

      {/* Exams Table / Cards */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading exams data...</p>
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="file-text" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No exams match your search criteria</h3>
          <p className="text-xs text-slate-400">Try adjusting your filters or create a new examination</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Exam Name</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Format</th>
                  <th className="py-3 px-4">Date & Time</th>
                  <th className="py-3 px-4">Duration</th>
                  <th className="py-3 px-4">Marks</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredExams.map((exam) => {
                  const examId = exam.exam_id || exam.examId;
                  const isMcq = (exam.exam_type || exam.examType || "MCQ").toUpperCase() === "MCQ";
                  const status = exam.status || "Upcoming";

                  return (
                    <tr key={examId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        {exam.exam_name || exam.examName}
                      </td>
                      <td className="py-3 px-4">
                        <span className="text-xs font-medium text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
                          {exam.subject?.subject_name || exam.subject?.subjectName || "General"}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-1 rounded-md ${
                            isMcq ? "bg-orange-100 text-orange-700" : "bg-blue-100 text-blue-700"
                          }`}
                        >
                          {isMcq ? "MCQ" : "Q&A (Theory)"}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-xs text-slate-500">
                        <div>{exam.date?.split("T")[0] || exam.date}</div>
                        <div className="text-slate-400">
                          {exam.start_time || exam.startTime} - {exam.end_time || exam.endTime}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-xs font-medium text-slate-600">
                        {exam.duration_minutes || exam.durationMinutes || 60}m
                      </td>
                      <td className="py-3 px-4 text-xs">
                        <span className="font-semibold text-slate-800">
                          {exam.passing_marks || exam.passingMarks || 0}
                        </span>
                        <span className="text-slate-400"> / {exam.total_marks || exam.totalMarks || 100}</span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${
                            status === "Upcoming" || status === "Approved"
                              ? "bg-emerald-100 text-emerald-800"
                              : status === "Completed"
                              ? "bg-slate-100 text-slate-700"
                              : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleViewQuestions(exam)}
                            className="p-1.5 text-slate-600 hover:text-orange-600 hover:bg-orange-50 rounded-lg transition-colors text-xs font-medium flex items-center gap-1"
                            title="View Questions"
                          >
                            <Icon name="eye" className="w-4 h-4" />
                            View
                          </button>
                          <button
                            onClick={() => handleDelete(examId)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                            title="Delete Exam"
                          >
                            <Icon name="trash" className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Questions Modal */}
      {selectedExam && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[85vh] flex flex-col overflow-hidden border border-slate-200">
            <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <div>
                <h3 className="font-bold text-slate-800 text-base">
                  {selectedExam.exam_name || selectedExam.examName} - Question Set
                </h3>
                <p className="text-xs text-slate-500">
                  Format: {(selectedExam.exam_type || selectedExam.examType || "MCQ").toUpperCase()} | Total Questions: {examQuestions.length}
                </p>
              </div>
              <button
                onClick={() => setSelectedExam(null)}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg"
              >
                <Icon name="x" className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto flex-1 space-y-4">
              {loadingQuestions ? (
                <div className="py-10 text-center">
                  <div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  <p className="text-xs text-slate-500">Fetching questions...</p>
                </div>
              ) : examQuestions.length === 0 ? (
                <p className="text-center text-sm text-slate-500 py-6">No questions found for this exam.</p>
              ) : (
                examQuestions.map((q, idx) => (
                  <div key={q.question_id || q.questionId || idx} className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                    <div className="flex items-start gap-2">
                      <span className="font-bold text-orange-600 text-sm">Q{idx + 1}.</span>
                      <p className="text-sm font-semibold text-slate-800">{q.question}</p>
                    </div>

                    {q.option1 || q.option2 ? (
                      <div className="grid grid-cols-2 gap-2 text-xs pt-1">
                        {[q.option1, q.option2, q.option3, q.option4].filter(Boolean).map((opt, oIdx) => {
                          const isCorrect = opt === (q.correct_option || q.correctOption);
                          return (
                            <div
                              key={oIdx}
                              className={`p-2 rounded-lg border ${
                                isCorrect
                                  ? "bg-emerald-50 border-emerald-300 text-emerald-800 font-bold"
                                  : "bg-white border-slate-200 text-slate-600"
                              }`}
                            >
                              <span className="text-slate-400 mr-1">{String.fromCharCode(65 + oIdx)}:</span> {opt}
                              {isCorrect && " (Correct)"}
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="text-xs bg-white p-2.5 rounded-lg border border-slate-200 text-slate-600">
                        <span className="font-semibold text-slate-700">Sample/Reference Answer:</span>{" "}
                        {q.sample_answer || q.sampleAnswer || "N/A"}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>

            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={() => setSelectedExam(null)}
                className="py-2 px-4 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-xl"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
