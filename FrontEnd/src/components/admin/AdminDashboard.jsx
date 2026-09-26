import React, { useState } from "react";
import AdminOverview from "./AdminOverview";
import RegisterUser from "./RegisterUser";
import TotalUsers from "./TotalUsers";
import TotalExams from "./TotalExams";
import AcceptExam from "./AcceptExam";
import ManageSubjects from "./ManageSubjects";
import ManageQuestions from "./ManageQuestions";
import ManageResults from "./ManageResults";
import { Icon } from "../common/Icons";

const ADMIN_TABS = [
  { id: "OVERVIEW", label: "Executive Dashboard", icon: "grid" },
  { id: "REQUESTS", label: "Registration Requests", icon: "user-plus" },
  { id: "USERS", label: "User Directory", icon: "users" },
  { id: "EXAMS", label: "Exams", icon: "clock" },
  { id: "QUESTIONS", label: "Question Bank", icon: "help-circle" },
  { id: "SUBJECTS", label: "Curriculum Subjects", icon: "book" },
  { id: "RESULTS", label: "Exam Results", icon: "award" },
  { id: "STATS", label: "Distribution Analytics", icon: "chart" },
];

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState("OVERVIEW");

  return (
    <div className="flex flex-col lg:flex-row gap-6">
      <nav
        aria-label="Admin sections"
        className="w-full lg:w-64 shrink-0 self-start bg-white border border-slate-200 rounded-2xl p-3 shadow-sm flex lg:flex-col gap-1.5 overflow-x-auto lg:overflow-visible scrollbar-thin"
      >
        {ADMIN_TABS.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              aria-current={isActive ? "page" : undefined}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap text-left ${
                isActive
                  ? "bg-orange-500 text-white shadow-md shadow-orange-500/20"
                  : "text-slate-600 hover:bg-orange-50 hover:text-orange-700"
              }`}
            >
              <Icon
                name={tab.icon}
                className="w-5 h-5 shrink-0"
                fill="white"
                stroke="black"
                strokeWidth={1.5}
              />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="flex-1 min-w-0 transition-opacity duration-200">
        {activeTab === "OVERVIEW" && <AdminOverview setActiveTab={setActiveTab} />}
        {activeTab === "REQUESTS" && <RegisterUser />}
        {activeTab === "USERS" && <TotalUsers />}
        {activeTab === "EXAMS" && <AcceptExam />}
        {activeTab === "QUESTIONS" && <ManageQuestions />}
        {activeTab === "SUBJECTS" && <ManageSubjects />}
        {activeTab === "RESULTS" && <ManageResults />}
        {activeTab === "STATS" && <TotalExams />}
      </div>
    </div>
  );
}
