import React, { useState, useEffect, useCallback } from "react";
import { PieChart, Pie, Tooltip, Legend, Cell, ResponsiveContainer } from "recharts";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export const isExamDateTimePassed = (exam) => {
  if (!exam) return false;
  if (exam.status === "Completed") return true;
  const dateStr = (exam.date || exam.exam_date || "").split("T")[0];
  if (!dateStr) return false;
  const endTimeStr = exam.end_time || exam.endTime || "23:59:59";
  try {
    const [year, month, day] = dateStr.split("-").map(Number);
    const timeParts = endTimeStr.split(":").map(Number);
    const hours = timeParts[0] || 0;
    const minutes = timeParts[1] || 0;
    const seconds = timeParts[2] || 0;
    const examEndTime = new Date(year, month - 1, day, hours, minutes, seconds);
    return examEndTime < new Date();
  } catch {
    return false;
  }
};

export default function TotalExams() {
  const [upcomingExams, setUpcomingExams] = useState([]);
  const [completedExams, setCompletedExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeView, setActiveView] = useState("UPCOMING"); // UPCOMING or COMPLETED
  const { showError } = useToast();

  const fetchExamsData = useCallback(async () => {
    setLoading(true);
    try {
      const [upRes, compRes] = await Promise.all([
        apiClient.get("/api/exams/viewAllUpComingExam"),
        apiClient.get("/api/exams/viewAllCompletedExam"),
      ]);

      const rawUpcoming = Array.isArray(upRes.data) ? upRes.data : [];
      const rawCompleted = Array.isArray(compRes.data) ? compRes.data : [];

      const genuinelyUpcoming = [];
      const allCompleted = [...rawCompleted];

      rawUpcoming.forEach((e) => {
        if (isExamDateTimePassed(e)) {
          const id = e.exam_id || e.examId;
          if (!allCompleted.some((c) => (c.exam_id || c.examId) === id)) {
            allCompleted.push({ ...e, status: "Completed" });
          }
        } else {
          genuinelyUpcoming.push(e);
        }
      });

      setUpcomingExams(genuinelyUpcoming);
      setCompletedExams(allCompleted);
    } catch (err) {
      showError(err.message || "Failed to load examination statistics");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchExamsData();
  }, [fetchExamsData]);

  const chartData = [
    { name: "Upcoming Exams", value: upcomingExams.length },
    { name: "Completed Exams", value: completedExams.length },
  ];
  const COLORS = ["#ea580c", "#0f172a"];

  const currentList = activeView === "UPCOMING" ? upcomingExams : completedExams;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="chart" className="w-5 h-5 text-orange-500" />
          Examination Distribution & Analytics
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Comprehensive statistics on scheduled upcoming and finalized examinations
        </p>
      </div>

      {/* Stats Cards & Chart */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
            <Icon name="clock" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Upcoming Exams</span>
            <p className="text-2xl font-black text-orange-600 mt-0.5">{upcomingExams.length}</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center">
            <Icon name="check-circle" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Completed Exams</span>
            <p className="text-2xl font-black text-slate-900 mt-0.5">{completedExams.length}</p>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center h-28">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius={25}
                outerRadius={45}
                paddingAngle={5}
                dataKey="value"
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend verticalAlign="middle" align="right" layout="vertical" iconSize={8} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Switcher & Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden p-4 sm:p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex bg-slate-100 p-1 rounded-xl">
            <button
              onClick={() => setActiveView("UPCOMING")}
              className={`px-4 py-2 text-xs font-bold rounded-lg transition-all ${
                activeView === "UPCOMING" ? "bg-white text-orange-600 shadow-sm" : "text-slate-500 hover:text-slate-800"
              }`}
            >
              Upcoming ({upcomingExams.length})
            </button>
            <button
              onClick={() => setActiveView("COMPLETED")}
              className={`px-4 py-2 text-xs font-bold rounded-lg transition-all ${
                activeView === "COMPLETED" ? "bg-white text-orange-600 shadow-sm" : "text-slate-500 hover:text-slate-800"
              }`}
            >
              Completed ({completedExams.length})
            </button>
          </div>
        </div>

        {loading ? (
          <div className="py-12 text-center">
            <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
            <p className="text-xs text-slate-500">Loading exams data...</p>
          </div>
        ) : currentList.length === 0 ? (
          <div className="text-center py-10 space-y-2">
            <p className="text-sm font-semibold text-slate-600">No {activeView.toLowerCase()} exams recorded</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Exam ID</th>
                  <th className="py-3 px-4">Exam Name</th>
                  <th className="py-3 px-4">Exam Date</th>
                  <th className="py-3 px-4">Start Time</th>
                  <th className="py-3 px-4">End Time</th>
                  <th className="py-3 px-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {currentList.map((exam, idx) => {
                  const isPassed = isExamDateTimePassed(exam);
                  const displayStatus = isPassed ? "Completed" : (exam.status || "Upcoming");

                  return (
                    <tr key={exam.exam_id || exam.examId || idx} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-4 font-mono text-xs text-slate-400">#{exam.exam_id || exam.examId}</td>
                      <td className="py-3 px-4 font-semibold text-slate-900">{exam.exam_name || exam.examName}</td>
                      <td className="py-3 px-4 text-xs text-slate-600">{exam.date?.split("T")[0] || exam.date}</td>
                      <td className="py-3 px-4 text-xs text-slate-500">{exam.start_time || exam.startTime || "10:00"}</td>
                      <td className="py-3 px-4 text-xs text-slate-500">{exam.end_time || exam.endTime || "11:00"}</td>
                      <td className="py-3 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full inline-flex items-center gap-1 ${
                            displayStatus === "Completed"
                              ? "bg-slate-100 text-slate-700"
                              : "bg-emerald-100 text-emerald-800"
                          }`}
                        >
                          {displayStatus === "Completed" ? (
                            <Icon name="check-circle" className="w-3 h-3 text-slate-600" />
                          ) : (
                            <Icon name="clock" className="w-3 h-3 text-emerald-600" />
                          )}
                          {displayStatus}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
