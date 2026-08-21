import React from "react";
import { Icon } from "../common/Icons";

export default function Values() {
  const values = [
    {
      title: "Academic Integrity",
      description: "We champion fair, honest, and transparent testing environments. Protecting credential credibility is our standard.",
      icon: "check-circle",
    },
    {
      title: "Pedagogical Empowerment",
      description: "We equip educators with intuitive question authoring tools and provide students with clear learning diagnostics.",
      icon: "award",
    },
    {
      title: "Technological Innovation",
      description: "Continuous development of modern assessment paradigms that blend automated testing with deep subjective evaluation.",
      icon: "chart",
    },
    {
      title: "System Reliability",
      description: "Resilient infrastructure crafted to handle concurrent exam sessions smoothly with autosaved draft protection.",
      icon: "clock",
    },
  ];

  return (
    <div className="space-y-6 pt-6">
      <div className="inline-block px-3 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-lg uppercase tracking-wider">
        Guiding Principles
      </div>
      <h2 className="text-3xl font-black text-slate-900 tracking-tight">
        Our Core Values
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {values.map((v, i) => (
          <div key={i} className="p-5 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-2">
            <div className="flex items-center gap-2.5 text-orange-500 font-bold text-base">
              <Icon name={v.icon} className="w-5 h-5" />
              <span className="text-slate-900">{v.title}</span>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed">{v.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
