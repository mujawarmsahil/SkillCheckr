import React from "react";

export default function History() {
  return (
    <div className="space-y-6">
      <div className="inline-block px-3 py-1 bg-orange-100 text-orange-700 text-xs font-bold rounded-lg uppercase tracking-wider">
        Our Evolution
      </div>
      <h2 className="text-3xl font-black text-slate-900 tracking-tight">
        How <span className="text-orange-500">SkillCheckr</span> Evolved
      </h2>
      <p className="text-base text-slate-600 leading-relaxed">
        <strong>SkillCheckr</strong> was built to simplify assessment for teachers, students, and institutions. It handles both multiple-choice exams and written-answer questions, which most legacy tools force you to choose between.
      </p>
      <p className="text-base text-slate-600 leading-relaxed">
        You can set exam schedules, publish proctoring rules, and generate results automatically.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4">
        <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl">
          <h4 className="font-bold text-slate-800 text-sm">Flexible Formats</h4>
          <p className="text-xs text-slate-500 mt-1">Supports MCQ and written answers.</p>
        </div>
        <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl">
          <h4 className="font-bold text-slate-800 text-sm">Instant Scoring</h4>
          <p className="text-xs text-slate-500 mt-1">Automated grade calculation and question breakdown.</p>
        </div>
        <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl">
          <h4 className="font-bold text-slate-800 text-sm">Institutional Security</h4>
          <p className="text-xs text-slate-500 mt-1">Verified role access and administrative supervision.</p>
        </div>
      </div>
    </div>
  );
}
