import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { isExamDateTimePassed } from "../../utils/examUtils";

export default function AcceptExam() {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL"); // "ALL", "PENDING", "UPCOMING", "COMPLETED", "CANCELLED"
  const { showSuccess, showError } = useToast();

  const fetchExams = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/exams/viewAllExams");
      setExams(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      if (err.status === 404 || err.response?.status === 404) {
        setExams([]);
      } else {
        showError(err.message || "Failed to load exams");
      }
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchExams();
  }, [fetchExams]);

  const handleApproveExam = async (examId) => {
    try {
      await apiClient.post(`/api/exams/approve/${examId}`);
      showSuccess("Exam approved & scheduled for student registration!");
      setExams((prev) =>
        prev.map((e) => ((e.exam_id === examId || e.examId === examId) ? { ...e, status: "Upcoming" } : e))
      );
    } catch (err) {
      showError(err.message || "Failed to approve exam");
    }
  };

  const handleRejectExam = async (examId) => {
    if (!window.confirm("Are you sure you want to reject this proposed examination?")) return;
    try {
      await apiClient.post(`/api/exams/reject/${examId}`);
      showSuccess("Exam marked as rejected");
      setExams((prev) =>
        prev.map((e) => ((e.exam_id === examId || e.examId === examId) ? { ...e, status: "Rejected" } : e))
      );
    } catch (err) {
      showError(err.message || "Failed to reject exam");
    }
  };

  const handleCancelExam = async (examId) => {
    if (!window.confirm("Are you sure you want to cancel this scheduled exam? Enrolled students will not be able to attend.")) return;
    try {
      await apiClient.post(`/api/exams/cancel/${examId}`);
      showSuccess("Exam cancelled successfully");
      setExams((prev) =>
        prev.map((e) => ((e.exam_id === examId || e.examId === examId) ? { ...e, status: "Cancelled" } : e))
      );
    } catch (err) {
      showError(err.message || "Failed to cancel exam");
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

  const getComputedExamStatus = (exam) => {
    const rawStatus = (exam.status || "Pending").trim();
    if (rawStatus.toLowerCase() === "pending") return "Pending";
    if (rawStatus.toLowerCase() === "rejected") return "Rejected";
    if (rawStatus.toLowerCase() === "cancelled") return "Cancelled";
    if (rawStatus.toLowerCase() === "completed") return "Completed";
    if (isExamDateTimePassed(exam)) return "Completed";
    return "Upcoming";
  };

  const filteredExams = exams.filter((e) => {
    const title = (e.exam_name || e.examName || "").toLowerCase();
    const sub = (e.subject?.subject_name || e.subject?.subjectName || "").toLowerCase();
    const query = search.trim().toLowerCase();
    const matchesSearch = !query || title.includes(query) || sub.includes(query);

    const computedStatus = getComputedExamStatus(e).toUpperCase();
    let matchesFilter = filterStatus === "ALL" || computedStatus === filterStatus;
    if (filterStatus === "CANCELLED") {
      matchesFilter = computedStatus === "CANCELLED" || computedStatus === "REJECTED";
    }

    return matchesSearch && matchesFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="clock" className="w-5 h-5 text-orange-500" />
          Examination Approvals & Lifecycle Management
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Review educator-created assessments, approve schedules, cancel ongoing/upcoming tests, and manage statuses
        </p>
      </div>

      {/* Search & Filter Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative flex-1 w-full">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search exams by title or subject..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        {/* Status Filters */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {[
            { id: "ALL", label: "All Exams" },
            { id: "PENDING", label: "Pending Review" },
            { id: "UPCOMING", label: "Upcoming" },
            { id: "COMPLETED", label: "Completed" },
            { id: "CANCELLED", label: "Cancelled / Rejected" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterStatus(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                filterStatus === tab.id
                  ? "bg-slate-900 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Exams Table */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading examinations...</p>
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3 shadow-sm">
          <Icon name="check-circle" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No exams match your criteria</h3>
          <p className="text-xs text-slate-400">All submitted assessments have been updated or filtered.</p>
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
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredExams.map((exam) => {
                  const examId = exam.exam_id || exam.examId;
                  const computedStatus = getComputedExamStatus(exam);
                  const isPending = computedStatus === "Pending";
                  const isUpcoming = computedStatus === "Upcoming";
                  const isCompleted = computedStatus === "Completed";
                  const isCancelled = computedStatus === "Cancelled" || computedStatus === "Rejected";

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
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full inline-flex items-center gap-1 ${
                            isUpcoming
                              ? "bg-emerald-100 text-emerald-800"
                              : isCompleted
                              ? "bg-slate-100 text-slate-700"
                              : isCancelled
                              ? "bg-rose-100 text-rose-800"
                              : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {isUpcoming && <Icon name="clock" className="w-3 h-3 text-emerald-600" />}
                          {isCompleted && <Icon name="check-circle" className="w-3 h-3 text-slate-600" />}
                          {isPending && <Icon name="alert" className="w-3 h-3 text-amber-600" />}
                          {isCancelled && <Icon name="x" className="w-3 h-3 text-rose-600" />}
                          {computedStatus}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {isPending && (
                            <>
                              <button
                                onClick={() => handleApproveExam(examId)}
                                className="py-1 px-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-all flex items-center gap-1"
                              >
                                <Icon name="check" className="w-3.5 h-3.5" />
                                Approve
                              </button>
                              <button
                                onClick={() => handleRejectExam(examId)}
                                className="py-1 px-2.5 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-semibold rounded-lg transition-all"
                              >
                                Reject
                              </button>
                            </>
                          )}

                          {isUpcoming && (
                            <button
                              onClick={() => handleCancelExam(examId)}
                              className="py-1 px-2.5 bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-semibold rounded-lg transition-all flex items-center gap-1"
                              title="Cancel Scheduled Exam"
                            >
                              <Icon name="alert" className="w-3.5 h-3.5" />
                              Cancel
                            </button>
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
