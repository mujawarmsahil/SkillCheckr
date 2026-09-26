import React from "react";
import { Link } from "react-router-dom";
import Logo from "../../assets/images/logo/white_logo.png";

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-slate-950 text-slate-300 border-t border-slate-800">
      <div className="w-full max-w-[1536px] mx-auto px-4 sm:px-8 lg:px-12 py-14">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          <div className="space-y-4">
            <Link to="/" className="inline-block">
              <img src={Logo} alt="SkillCheckr" className="h-11 w-auto object-contain" />
            </Link>
            <p className="text-sm text-slate-400 leading-relaxed">
              MCQ and written-answer exams for students, teachers, and institutions.
            </p>
          </div>

          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Platform</h3>
            <ul className="space-y-3 text-sm">
              <li>
                <Link to="/" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Home
                </Link>
              </li>
              <li>
                <Link to="/about" className="text-slate-400 hover:text-orange-400 transition-colors">
                  About SkillCheckr
                </Link>
              </li>
              <li>
                <Link to="/blog" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Blog & Insights
                </Link>
              </li>
              <li>
                <Link to="/contact" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Contact & Support
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Access</h3>
            <ul className="space-y-3 text-sm">
              <li>
                <Link to="/authentication" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Student Portal
                </Link>
              </li>
              <li>
                <Link to="/authentication" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Teacher Portal
                </Link>
              </li>
              <li>
                <Link to="/authentication" className="text-slate-400 hover:text-orange-400 transition-colors">
                  Admin Console
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Connect</h3>
            <p className="text-sm text-slate-400 mb-4">
              Need assistance or institutional onboarding? Reach out to our team.
            </p>
            <Link
              to="/contact"
              className="inline-block py-2.5 px-5 bg-slate-800 hover:bg-orange-600 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
            >
              Get in Touch
            </Link>
          </div>
        </div>

        <div className="mt-14 pt-8 border-t border-slate-800/80 flex flex-col sm:flex-row justify-between items-center gap-4 text-xs text-slate-500">
          <p>© {currentYear} SkillCheckr. All rights reserved.</p>
          <div className="flex gap-6">
            <span>Privacy Policy</span>
            <span>Terms of Service</span>
            <span>Security Architecture</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
