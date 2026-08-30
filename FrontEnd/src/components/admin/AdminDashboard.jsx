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

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState("OVERVIEW");

  const tabs = [
    { id: "OVERVIEW", label: "Executive Dashboard", icon: "grid" },
    { id: "REQUESTS", label: "Registration Requests", icon: "user-plus" },
    { id: "USERS", label: "User Directory", icon: "users" },
    { id: "EXAMS", label: "Examinations", icon: "clock" },
    { id: "QUESTIONS", label: "Question Bank", icon: "help-circle" },
    { id: "SUBJECTS", label: "Curriculum Subjects", icon: "book" },
    { id: "RESULTS", label: "Exam Results", icon: "award" },
    { id: "STATS", label: "Distribution Analytics", icon: "chart" },
  ];

  return (
    <div className="space-y-6">
      {/* Tab Navigation */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-200 scrollbar-thin">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === tab.id
                ? "bg-slate-900 text-white shadow-md shadow-slate-900/10"
                : "bg-white text-slate-600 hover:bg-slate-100 hover:text-slate-900 border border-slate-200"
            }`}
          >
            <Icon name={tab.icon} className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Active Tab Content */}
      <div className="transition-opacity duration-200">
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
