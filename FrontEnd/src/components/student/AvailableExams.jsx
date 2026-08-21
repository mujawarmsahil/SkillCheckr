import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function AvailableExams() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterType, setFilterType] = useState("ALL"); // ALL, MCQ, QUESTION_ANSWER
  const { showError } = useToast();
  const navigate = useNavigate();

  const fetchUpcomingExams = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/exams/viewAllUpComingExam");
      setExams(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      showError(err.message || "Failed to fetch upcoming exams");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchUpcomingExams();
  }, [fetchUpcomingExams]);

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
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="book" className="w-5 h-5 text-orange-500" />
          Available Examinations
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Select an active examination to test your proficiency
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
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto">
          {[
            { id: "ALL", label: "All Formats" },
            { id: "MCQ", label: "MCQ Exams" },
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
          <p className="text-xs text-slate-500">Loading available examinations...</p>
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="book" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No active examinations currently available</h3>
          <p className="text-xs text-slate-400">Check back later or contact your instructor for scheduled tests.</p>
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

            return (
              <div
                key={examId}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all p-5 flex flex-col justify-between space-y-4"
              >
                {/* Card Header */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-bold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-md">
                      {subjectName}
                    </span>
                    <span
                      className={`text-xs font-bold px-2.5 py-1 rounded-md ${
                        isMcq ? "bg-orange-100 text-orange-700" : "bg-blue-100 text-blue-700"
                      }`}
                    >
                      {isMcq ? "MCQ Exam" : "Q&A (Theory)"}
                    </span>
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
                  <div className="col-span-2 text-slate-500 flex items-center gap-1.5 pt-1 border-t border-slate-200/60">
                    <span>Date: {exam.date?.split("T")[0] || exam.date}</span>
                    <span>({exam.start_time || exam.startTime || "10:00"})</span>
                  </div>
                </div>

                {/* Start Exam CTA */}
                <button
                  onClick={() =>
                    navigate(`/take-exam/${examId}`, {
                      state: {
                        examId,
                        examName: exam.exam_name || exam.examName,
                        examType,
                        subjectName,
                        durationMinutes: duration,
                        totalMarks,
                        passingMarks: passMarks,
                      },
                    })
                  }
                  className="w-full py-2.5 px-4 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-semibold rounded-xl text-sm shadow-sm transition-all flex items-center justify-center gap-2"
                >
                  Start Examination →
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
