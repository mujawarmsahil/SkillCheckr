import React, { useState } from "react";
import AddExam from "./AddExam";
import ManageExams from "./ManageExams";
import { Icon } from "../common/Icons";

export default function TeacherDashboard() {
  const [activeTab, setActiveTab] = useState("MANAGE");

  const tabs = [
    { id: "MANAGE", label: "My Exams", icon: "file-text" },
    { id: "ADD_EXAM", label: "Add Exam", icon: "plus" },
  ];

  return (
    <div className="space-y-6">
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

      <div className="transition-opacity duration-200">
        {activeTab === "MANAGE" && <ManageExams onAddNew={() => setActiveTab("ADD_EXAM")} />}
        {activeTab === "ADD_EXAM" && <AddExam onExamCreated={() => setActiveTab("MANAGE")} />}
      </div>
    </div>
  );
}
