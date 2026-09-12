import React, { useState, useEffect, useCallback } from "react";
import { PieChart, Pie, Tooltip, Cell, ResponsiveContainer } from "recharts";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";
import { isExamDateTimePassed } from "../../utils/examUtils";

export default function TotalExams() {
  const [upcomingExams, setUpcomingExams] = useState([]);
  const [completedExams, setCompletedExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeView, setActiveView] = useState("UPCOMING"); // UPCOMING or COMPLETED
  const { showError } = useToast();

  const fetchExamsData = useCallback(async () => {
    setLoading(true);
    try {
      const upPromise = apiClient.get("/api/exams/viewAllUpComingExam").catch((err) => {
        if (err.status === 404 || err.response?.status === 404) return { data: [] };
        throw err;
      });
      const compPromise = apiClient.get("/api/exams/viewAllCompletedExam").catch((err) => {
        if (err.status === 404 || err.response?.status === 404) return { data: [] };
        throw err;
      });
      const [upRes, compRes] = await Promise.all([upPromise, compPromise]);

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

  const totalExams = upcomingExams.length + completedExams.length;
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
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4 min-h-[120px]">
          <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center flex-shrink-0">
            <Icon name="clock" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Upcoming Exams</span>
            <p className="text-2xl font-black text-orange-600 mt-0.5">{upcomingExams.length}</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4 min-h-[120px]">
          <div className="w-12 h-12 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center flex-shrink-0">
            <Icon name="check-circle" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Completed Exams</span>
            <p className="text-2xl font-black text-slate-900 mt-0.5">{completedExams.length}</p>
          </div>
        </div>

        <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center min-h-[120px]">
          {totalExams === 0 ? (
            <div className="w-full flex flex-col items-center justify-center text-slate-400 py-2">
              <Icon name="chart" className="w-6 h-6 mb-1 text-slate-300" />
              <span className="text-xs font-medium">No exam data available</span>
            </div>
          ) : (
            <div className="w-full flex items-center justify-between gap-3">
              {/* Donut Chart */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 flex-shrink-0 relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart margin={{ top: 0, right: 0, bottom: 0, left: 0 }}>
                    <Pie
                      data={chartData}
                      cx="50%"
                      cy="50%"
                      innerRadius={24}
                      outerRadius={42}
                      paddingAngle={chartData.filter((d) => d.value > 0).length > 1 ? 4 : 0}
                      dataKey="value"
                    >
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index]} />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload }) => {
                        if (active && payload && payload.length) {
                          const data = payload[0];
                          return (
                            <div className="bg-slate-900 text-white px-2.5 py-1 rounded-lg text-xs shadow-md font-medium">
                              <span>{data.name}: </span>
                              <span className="font-bold">{data.value}</span>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              {/* Breakdown Legend */}
              <div className="flex flex-col justify-center gap-1.5 flex-1 min-w-0">
                <div className="flex items-center justify-between pb-1 border-b border-slate-100">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Breakdown</span>
                  <span className="text-[11px] font-bold text-slate-700">{totalExams} Total</span>
                </div>
                {chartData.map((item, idx) => {
                  const pct = totalExams > 0 ? Math.round((item.value / totalExams) * 100) : 0;
                  return (
                    <div key={item.name} className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-1.5 min-w-0">
                        <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ backgroundColor: COLORS[idx] }} />
                        <span className="text-slate-600 font-medium truncate text-xs">{item.name}</span>
                      </div>
                      <div className="flex items-center gap-1 pl-1 flex-shrink-0">
                        <span className="font-bold text-slate-900">{item.value}</span>
                        <span className="text-[10px] text-slate-400 font-medium">({pct}%)</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
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
