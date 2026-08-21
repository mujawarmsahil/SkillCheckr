import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Logo from "../../assets/images/logo/logo.png";
import Login from "./Login";
import Signup from "./Signup";
import { useAuth } from "../../context/AuthContext";
import { Icon } from "../common/Icons";

export default function Authentication() {
  const [activeTab, setActiveTab] = useState("login");
  const { isAuthenticated, role, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <section className="min-h-screen w-full bg-slate-950 flex flex-col justify-center items-center p-4 relative overflow-hidden">
      {/* Background Decorative Gradients */}
      <div className="absolute top-1/4 -left-20 w-80 h-80 bg-orange-500/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 -right-20 w-80 h-80 bg-blue-500/10 rounded-full blur-3xl pointer-events-none"></div>

      {/* Brand Header */}
      <div className="mb-6 flex flex-col items-center">
        <Link to="/" className="flex items-center gap-2 hover:opacity-90 transition-opacity">
          <img src={Logo} alt="SkillCheckr" className="h-10 object-contain" />
        </Link>
      </div>

      {/* Auth Card */}
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-100 p-6 sm:p-8 relative z-10">
        {isAuthenticated ? (
          <div className="text-center py-6">
            <div className="w-16 h-16 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Icon name="check-circle" className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-bold text-slate-800">You are already signed in</h2>
            <p className="text-sm text-slate-500 mt-1 mb-6">
              Active Role: <span className="font-semibold text-orange-600 capitalize">{role}</span>
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <button
                onClick={() => navigate(`/dashboard/${(role || "student").toLowerCase()}`)}
                className="py-2.5 px-5 bg-orange-500 hover:bg-orange-600 text-white font-medium rounded-xl shadow transition-all"
              >
                Go to Dashboard
              </button>
              <button
                onClick={logout}
                className="py-2.5 px-5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-xl transition-all"
              >
                Sign Out
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Tab Switcher */}
            <div className="flex bg-slate-100 p-1 rounded-xl">
              <button
                type="button"
                onClick={() => setActiveTab("login")}
                className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-all ${
                  activeTab === "login"
                    ? "bg-white text-orange-600 shadow-sm"
                    : "text-slate-500 hover:text-slate-800"
                }`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => setActiveTab("signup")}
                className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-all ${
                  activeTab === "signup"
                    ? "bg-white text-orange-600 shadow-sm"
                    : "text-slate-500 hover:text-slate-800"
                }`}
              >
                Register
              </button>
            </div>

            {/* Subtitle */}
            <div className="mt-4 text-center">
              <h2 className="text-xl font-bold text-slate-800">
                {activeTab === "login" ? "Welcome back" : "Create an Account"}
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                {activeTab === "login"
                  ? "Enter your credentials to access your dashboard"
                  : "Submit your registration request for Admin review"}
              </p>
            </div>

            {/* Forms */}
            {activeTab === "login" ? (
              <Login />
            ) : (
              <Signup onSignupSuccess={() => setActiveTab("login")} />
            )}
          </>
        )}
      </div>

      {/* Back to Home Link */}
      <div className="mt-6 text-center z-10">
        <Link to="/" className="text-xs text-slate-400 hover:text-slate-200 transition-colors">
          ← Back to Homepage
        </Link>
      </div>
    </section>
  );
}
