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
        <strong>SkillCheckr</strong> was founded with a clear mission: to elevate academic testing and simplify examination processes for educators, learners, and educational institutions. Recognizing the limitations of rigid legacy assessment tools, we created a dual-engine platform accommodating both high-speed multiple choice assessments and deep conceptual question-answer theory evaluations.
      </p>
      <p className="text-base text-slate-600 leading-relaxed">
        From customizable exam blueprints to transparent proctoring guidelines and automated scorecard generation, our solution is engineered to deliver excellence, fairness, and speed.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4">
        <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl">
          <h4 className="font-bold text-slate-800 text-sm">Flexible Formats</h4>
          <p className="text-xs text-slate-500 mt-1">Dual MCQ & Descriptive Question testing support.</p>
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
