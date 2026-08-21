import React, { useState } from "react";
import RegisterUser from "./RegisterUser";
import TotalUsers from "./TotalUsers";
import TotalExams from "./TotalExams";
import AcceptExam from "./AcceptExam";
import { Icon } from "../common/Icons";

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState("REQUESTS");

  const tabs = [
    { id: "REQUESTS", label: "Registration Requests", icon: "user-plus" },
    { id: "USERS", label: "User Directory", icon: "users" },
    { id: "ACCEPT_EXAM", label: "Exam Approvals", icon: "check" },
    { id: "STATS", label: "Exam Distribution", icon: "chart" },
  ];

  return (
    <div className="space-y-6">
      {/* Tab Navigation Navigation */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-200">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === tab.id
                ? "bg-slate-900 text-white shadow-md"
                : "bg-white text-slate-600 hover:bg-slate-100 border border-slate-200"
            }`}
          >
            <Icon name={tab.icon} className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Active Tab Content */}
      <div className="transition-opacity duration-200">
        {activeTab === "REQUESTS" && <RegisterUser />}
        {activeTab === "USERS" && <TotalUsers />}
        {activeTab === "ACCEPT_EXAM" && <AcceptExam />}
        {activeTab === "STATS" && <TotalExams />}
      </div>
    </div>
  );
}
