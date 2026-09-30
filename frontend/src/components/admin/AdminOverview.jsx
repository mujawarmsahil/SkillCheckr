import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function AdminOverview({ setActiveTab }) {
  const [stats, setStats] = useState({
    totalStudents: 0,
    totalTeachers: 0,
    totalExams: 0,
    pendingExams: 0,
    upcomingExams: 0,
    completedExams: 0,
    pendingRequests: 0,
    totalSubjects: 0,
    totalQuestions: 0,
    totalResults: 0,
  });
  const [loading, setLoading] = useState(true);
  const { showError } = useToast();

  const loadStats = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/admin/stats");
      if (res.data) {
        setStats(res.data);
      }
    } catch (err) {
      showError(err.message || "Failed to load dashboard metrics");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const cards = [
    {
      id: "students",
      label: "Students",
      value: stats.totalStudents || 0,
      icon: "users",
      color: "orange",
      actionTab: "USERS",
      desc: "Active student accounts",
    },
    {
      id: "teachers",
      label: "Teachers",
      value: stats.totalTeachers || 0,
      icon: "award",
      color: "blue",
      actionTab: "USERS",
      desc: "Registered teacher accounts",
    },
    {
      id: "requests",
      label: "Approvals",
      value: stats.pendingRequests || 0,
      icon: "user-plus",
      color: "amber",
      actionTab: "REQUESTS",
      desc: "Awaiting review",
      highlight: (stats.pendingRequests || 0) > 0,
    },
    {
      id: "exams",
      label: "Exams",
      value: stats.totalExams || 0,
      icon: "clock",
      color: "slate",
      actionTab: "EXAMS",
      desc: "Scheduled and completed",
    },
    {
      id: "upcomingExams",
      label: "Upcoming",
      value: stats.upcomingExams || 0,
      icon: "clock",
      color: "emerald",
      actionTab: "EXAMS",
      desc: "Scheduled ahead",
    },
    {
      id: "completedExams",
      label: "Completed",
      value: stats.completedExams || 0,
      icon: "check-circle",
      color: "indigo",
      actionTab: "STATS",
      desc: "Finished exams",
    },
    {
      id: "subjects",
      label: "Subjects",
      value: stats.totalSubjects || 0,
      icon: "book",
      color: "purple",
      actionTab: "SUBJECTS",
      desc: "Configured subjects",
    },
    {
      id: "questions",
      label: "Questions",
      value: stats.totalQuestions || 0,
      icon: "help-circle",
      color: "rose",
      actionTab: "QUESTIONS",
      desc: "Questions and rubrics",
    },
    {
      id: "results",
      label: "Results",
      value: stats.totalResults || 0,
      icon: "chart",
      color: "cyan",
      actionTab: "RESULTS",
      desc: "Attempts and scores",
    },
  ];

  const getColorClasses = (color) => {
    switch (color) {
      case "orange":
        return { bg: "bg-orange-50", text: "text-orange-600", border: "hover:border-orange-300" };
      case "blue":
        return { bg: "bg-blue-50", text: "text-blue-600", border: "hover:border-blue-300" };
      case "amber":
        return { bg: "bg-amber-50", text: "text-amber-600", border: "hover:border-amber-300" };
      case "emerald":
        return { bg: "bg-emerald-50", text: "text-emerald-600", border: "hover:border-emerald-300" };
      case "indigo":
        return { bg: "bg-indigo-50", text: "text-indigo-600", border: "hover:border-indigo-300" };
      case "purple":
        return { bg: "bg-purple-50", text: "text-purple-600", border: "hover:border-purple-300" };
      case "rose":
        return { bg: "bg-rose-50", text: "text-rose-600", border: "hover:border-rose-300" };
      case "cyan":
        return { bg: "bg-cyan-50", text: "text-cyan-600", border: "hover:border-cyan-300" };
      default:
        return { bg: "bg-slate-100", text: "text-slate-700", border: "hover:border-slate-300" };
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
        <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
          <Icon name="grid" className="w-4 h-4 text-orange-500" />
          Quick Actions
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {[
            { label: "User Directory", tab: "USERS", icon: "users" },
            { label: "Review Requests", tab: "REQUESTS", icon: "user-plus" },
            { label: "Manage Exams", tab: "EXAMS", icon: "clock" },
            { label: "Question Bank", tab: "QUESTIONS", icon: "help-circle" },
            { label: "Curriculum Subjects", tab: "SUBJECTS", icon: "book" },
            { label: "Results", tab: "RESULTS", icon: "award" },
          ].map((item) => (
            <button
              key={item.tab}
              onClick={() => setActiveTab && setActiveTab(item.tab)}
              className="p-3 bg-slate-50 hover:bg-orange-50 hover:border-orange-200 border border-slate-200 rounded-xl text-left transition-all group flex flex-col justify-between min-h-[80px]"
            >
              <Icon name={item.icon} className="w-5 h-5 text-slate-500 group-hover:text-orange-600 mb-2 transition-colors" />
              <span className="text-xs font-bold text-slate-700 group-hover:text-orange-700 leading-tight">
                {item.label}
              </span>
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading metrics...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {cards.map((card) => {
            const styles = getColorClasses(card.color);
            return (
              <div
                key={card.id}
                onClick={() => setActiveTab && setActiveTab(card.actionTab)}
                className={`bg-white p-5 rounded-2xl border border-slate-200 shadow-sm transition-all duration-200 cursor-pointer ${styles.border} hover:shadow-md relative overflow-hidden group`}
              >
                {card.highlight && (
                  <span className="absolute top-3 right-3 w-2.5 h-2.5 rounded-full bg-amber-500 ring-4 ring-amber-100"></span>
                )}
                <div className="flex items-center justify-between gap-4">
                  <div className="space-y-1">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block">
                      {card.label}
                    </span>
                    <p className="text-3xl font-black text-slate-900 tracking-tight">
                      {card.value}
                    </p>
                    <p className="text-[11px] text-slate-400">{card.desc}</p>
                  </div>
                  <div
                    className={`w-12 h-12 rounded-2xl ${styles.bg} ${styles.text} flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform`}
                  >
                    <Icon name={card.icon} className="w-6 h-6" />
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500 group-hover:text-orange-600 font-semibold transition-colors">
                  <span>Manage {card.label.split(" ")[0]}</span>
                  <Icon name="chevron-right" className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
