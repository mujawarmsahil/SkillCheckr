import React, { useState, useEffect, useCallback } from "react";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { getStudentResults } from "../../api/resultApi";
import { RESULT_STATUS } from "../../constants/resultConstants";
import { QUESTION_TYPES } from "../../constants/examConstants";

export default function StudentResults() {
  const { user } = useAuth();
  const { showError } = useToast();
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchResults = useCallback(async () => {
    setLoading(true);
    try {
      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;
      const data = await getStudentResults(studentId);
      setResults(data);
    } catch (err) {
      showError(err.message || "Failed to load examination results");
    } finally {
      setLoading(false);
    }
  }, [user?.roleId, showError]);

  useEffect(() => {
    fetchResults();
  }, [fetchResults]);

  const totalExams = results.length;
  const passedExams = results.filter((r) => r.status === RESULT_STATUS.PASS).length;
  const avgPercentage = totalExams > 0 ? (results.reduce((acc, r) => acc + (r.percentage || 0), 0) / totalExams).toFixed(1) : 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="chart" className="w-5 h-5 text-orange-500" />
          Examination Performance & Results
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Review your scorecard history and academic progress
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
            <Icon name="book" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Completed Tests</span>
            <p className="text-2xl font-black text-slate-900 mt-0.5">{totalExams}</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <Icon name="check-circle" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Passed Tests</span>
            <p className="text-2xl font-black text-emerald-600 mt-0.5">{passedExams}</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <Icon name="chart" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Average Score</span>
            <p className="text-2xl font-black text-blue-600 mt-0.5">{avgPercentage}%</p>
          </div>
        </div>
      </div>

      {/* Results Table */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading performance data...</p>
        </div>
      ) : results.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="award" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No examination attempts recorded yet</h3>
          <p className="text-xs text-slate-400">Complete an examination to view your verified results.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">Exam Name</th>
                  <th className="py-3.5 px-4">Subject</th>
                  <th className="py-3.5 px-4">Format</th>
                  <th className="py-3.5 px-4">Score</th>
                  <th className="py-3.5 px-4">Percentage</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Date Completed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {results.map((r, idx) => {
                  const isPass = r.status === RESULT_STATUS.PASS;
                  const isMcq = (r.exam_type || r.examType || QUESTION_TYPES.MCQ).toUpperCase() === QUESTION_TYPES.MCQ;
                  const marksObtained = r.marks_obtained ?? r.marksObtained;
                  const totalMarks = r.total_marks ?? r.totalMarks;
                  const percentage = r.percentage;
                  const examName = r.exam_name || r.examName || "Exam";
                  const subjectName = r.subject_name || r.subjectName || "General";
                  const submittedAt = r.submitted_at || r.submittedAt || "Recent";

                  return (
                    <tr key={r.result_id || r.resultId || idx} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-semibold text-slate-900">{examName}</td>
                      <td className="py-3.5 px-4">
                        <span className="text-xs font-medium bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                          {subjectName}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2 py-0.5 rounded-md ${
                            isMcq ? "bg-orange-100 text-orange-700" : "bg-blue-100 text-blue-700"
                          }`}
                        >
                          {isMcq ? "MCQ" : "Theory"}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium">
                        <strong className="text-slate-900">{marksObtained}</strong> / {totalMarks}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2">
                          <div className="w-16 bg-slate-100 h-2 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full ${isPass ? "bg-emerald-500" : r.status === RESULT_STATUS.SUBMITTED_FOR_EVALUATION ? "bg-blue-500" : "bg-rose-500"}`}
                              style={{ width: `${typeof percentage === "number" ? Math.min(100, percentage) : 0}%` }}
                            ></div>
                          </div>
                          <span className="text-xs font-bold text-slate-800">{percentage ?? "-"}{percentage !== undefined && "%"}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                            isPass
                              ? "bg-emerald-100 text-emerald-800"
                              : r.status === RESULT_STATUS.SUBMITTED_FOR_EVALUATION
                              ? "bg-blue-100 text-blue-800"
                              : "bg-rose-100 text-rose-800"
                          }`}
                        >
                          {r.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right text-xs text-slate-400">
                        {String(submittedAt).split("T")[0]}
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
