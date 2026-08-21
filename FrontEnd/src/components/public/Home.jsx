import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Card from "./Card";
import backgroundImage from "../../assets/images/backgroundImages/backgroundImage.jpg";
import { Icon } from "../common/Icons";

export default function Home() {
  const { isAuthenticated, role } = useAuth();

  const slogans = [
    "Assess Your Skills, Track Your Academic Progress!",
    "Unlock Your Full Potential with Adaptive Testing!",
    "Accurate MCQ & Descriptive Exam Management!",
    "Empowering Students, Teachers, and Institutions!",
  ];
  const [sloganIndex, setSloganIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setSloganIndex((prev) => (prev + 1) % slogans.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [slogans.length]);

  return (
    <div className="space-y-16">
      {/* Hero Section */}
      <section
        style={{ backgroundImage: `linear-gradient(rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0.85)), url(${backgroundImage})` }}
        className="w-full min-h-[85vh] bg-cover bg-center flex items-center justify-center text-white px-4 sm:px-6 lg:px-8 py-16"
      >
        <div className="max-w-4xl mx-auto text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-orange-500/20 border border-orange-500/40 text-orange-400 text-xs font-bold uppercase tracking-wider backdrop-blur-sm">
            <span>Next-Gen Examination Platform</span>
          </div>

          <h1 className="text-4xl sm:text-5xl md:text-6xl font-black tracking-tight leading-tight">
            Smart Academic Testing with <span className="text-orange-500">SkillCheckr</span>
          </h1>

          <div className="h-12 flex items-center justify-center">
            <p className="text-lg sm:text-xl font-medium text-slate-300 transition-opacity duration-300">
              {slogans[sloganIndex]}
            </p>
          </div>

          <p className="text-sm sm:text-base text-slate-400 max-w-2xl mx-auto leading-relaxed">
            Conduct seamless multiple-choice (MCQ) tests and comprehensive question-answer theory assessments with real-time analytics and instant grading.
          </p>

          {/* Action CTAs */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            {isAuthenticated ? (
              <Link
                to={`/dashboard/${(role || "student").toLowerCase()}`}
                className="w-full sm:w-auto py-3.5 px-8 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-bold rounded-2xl shadow-lg hover:shadow-orange-500/25 transition-all text-sm flex items-center justify-center gap-2"
              >
                <Icon name="book" className="w-5 h-5" />
                Go to {role} Dashboard →
              </Link>
            ) : (
              <>
                <Link
                  to="/authentication"
                  className="w-full sm:w-auto py-3.5 px-8 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-bold rounded-2xl shadow-lg hover:shadow-orange-500/25 transition-all text-sm flex items-center justify-center gap-2"
                >
                  <Icon name="book" className="w-5 h-5" />
                  Enter as Contender (Student)
                </Link>
                <Link
                  to="/authentication"
                  className="w-full sm:w-auto py-3.5 px-8 bg-slate-800 hover:bg-slate-700 active:bg-slate-900 border border-slate-700 text-white font-bold rounded-2xl shadow-lg transition-all text-sm flex items-center justify-center gap-2"
                >
                  <Icon name="users" className="w-5 h-5" />
                  Enter as Educator (Teacher)
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Feature Highlights Grid */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center max-w-2xl mx-auto mb-12 space-y-2">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
            Designed for Modern Academic Rigor
          </h2>
          <p className="text-sm text-slate-500">
            Flexible testing workflows built specifically for students, teachers, and administrators.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 flex items-center justify-center">
              <Icon name="check-circle" className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">MCQ Automated Testing</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Instant evaluation with automated scoring, question timers, answer key verification, and detailed scorecards.
            </p>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center">
              <Icon name="file-text" className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Question-Answer Theory</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Descriptive question support with live word counters, reference rubrics, draft autosaving, and evaluation workflows.
            </p>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <Icon name="chart" className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Institution & Admin Control</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Streamlined registration approval system, user management roster, exam approval pipelines, and platform analytics.
            </p>
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center max-w-2xl mx-auto mb-10 space-y-2">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
            Trusted by Institutions & Students
          </h2>
          <p className="text-sm text-slate-500">
            Hear from educators and learners using SkillCheckr
          </p>
        </div>
        <Card />
      </section>
    </div>
  );
}
