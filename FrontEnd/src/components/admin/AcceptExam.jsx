import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function AcceptExam() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const { showSuccess, showError } = useToast();

  const fetchExams = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/exams/viewAllExams");
      setExams(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      showError(err.message || "Failed to load exams");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchExams();
  }, [fetchExams]);

  const handleApproveExam = async (examId) => {
    try {
      await apiClient.post(`/api/exams/upComingExamStatus/${examId}`);
      showSuccess("Exam approved & activated for student access!");
      setExams((prev) =>
        prev.map((e) =>
          (e.exam_id === examId || e.examId === examId) ? { ...e, status: "Upcoming" } : e
        )
      );
    } catch (err) {
      showError(err.message || "Failed to approve exam");
    }
  };

  const handleDeleteExam = async (examId) => {
    if (!window.confirm("Are you sure you want to permanently delete this exam?")) return;
    try {
      await apiClient.delete(`/api/exams/deleteExamById/${examId}`);
      showSuccess("Exam deleted successfully");
      setExams((prev) => prev.filter((e) => e.exam_id !== examId && e.examId !== examId));
    } catch (err) {
      showError(err.message || "Failed to delete exam");
    }
  };

  const filteredExams = exams.filter((e) => {
    const title = (e.exam_name || e.examName || "").toLowerCase();
    const sub = (e.subject?.subject_name || e.subject?.subjectName || "").toLowerCase();
    return title.includes(search.toLowerCase()) || sub.includes(search.toLowerCase());
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="check" className="w-5 h-5 text-orange-500" />
          Examination Approval Console
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Review, approve, and activate educator-created assessments for student enrollment
        </p>
      </div>

      {/* Search */}
      <div className="flex items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative flex-1">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search exams by title or subject..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>
      </div>

      {/* Exams Table */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading examinations...</p>
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="check-circle" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No exams pending approval</h3>
          <p className="text-xs text-slate-400">All submitted tests have been processed.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">Exam ID</th>
                  <th className="py-3.5 px-4">Exam Title</th>
                  <th className="py-3.5 px-4">Subject</th>
                  <th className="py-3.5 px-4">Schedule</th>
                  <th className="py-3.5 px-4">Duration</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Approval Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredExams.map((exam) => {
                  const examId = exam.exam_id || exam.examId;
                  const isApproved = exam.status === "Upcoming" || exam.status === "Approved" || exam.status === "Completed";
                  const isPending = exam.status === "Pending" || !exam.status;

                  return (
                    <tr key={examId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-400">#{examId}</td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {exam.exam_name || exam.examName}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="text-xs font-medium bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                          {exam.subject?.subject_name || exam.subject?.subjectName || "General"}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-500">
                        <div>{exam.date?.split("T")[0] || exam.date}</div>
                        <div className="text-slate-400">{exam.start_time || exam.startTime} - {exam.end_time || exam.endTime}</div>
                      </td>
                      <td className="py-3.5 px-4 text-xs font-medium text-slate-600">
                        {exam.duration_minutes || exam.durationMinutes || 60}m
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                            isApproved ? "bg-emerald-100 text-emerald-800" : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {exam.status || "Pending"}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {isPending ? (
                            <button
                              onClick={() => handleApproveExam(examId)}
                              className="py-1.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-all flex items-center gap-1"
                            >
                              <Icon name="check" className="w-3.5 h-3.5" />
                              Approve
                            </button>
                          ) : (
                            <span className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
                              <Icon name="check-circle" className="w-4 h-4" />
                              Active
                            </span>
                          )}

                          <button
                            onClick={() => handleDeleteExam(examId)}
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
    </div>
  );
}
