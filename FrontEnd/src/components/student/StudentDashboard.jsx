import React, { useState } from "react";
import AvailableExams from "./AvailableExams";
import StudentResults from "./StudentResults";
import { Icon } from "../common/Icons";

export default function StudentDashboard() {
  const [activeTab, setActiveTab] = useState("AVAILABLE");

  const tabs = [
    { id: "AVAILABLE", label: "Available Exams", icon: "book" },
    { id: "RESULTS", label: "My Results & Scorecards", icon: "chart" },
  ];

  return (
    <div className="space-y-6">
      {/* Navigation Pills */}
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

      {/* Content */}
      <div className="transition-opacity duration-200">
        {activeTab === "AVAILABLE" && <AvailableExams />}
        {activeTab === "RESULTS" && <StudentResults />}
      </div>
    </div>
  );
}
