import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import WhiteLogo from "../../assets/images/logo/white_logo.png";
import Login from "./Login";
import Signup from "./Signup";
import { useAuth } from "../../context/AuthContext";
import { Icon } from "../common/Icons";

export default function Authentication() {
  const [activeTab, setActiveTab] = useState("login");
  const { isAuthenticated, role, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <section className="min-h-screen w-full bg-slate-950 flex flex-col justify-center items-center p-4 sm:p-6 relative overflow-hidden">
      <div className="absolute top-1/4 -left-20 w-96 h-96 bg-orange-500/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl pointer-events-none"></div>

      <div className="mb-8 flex flex-col items-center text-center relative z-10">
        <Link
          to="/"
          className="flex items-center justify-center group transition-transform hover:scale-105"
        >
          <img
            src={WhiteLogo}
            alt="SkillCheckr"
            className="h-14 sm:h-16 w-auto object-contain drop-shadow-[0_4px_24px_rgba(249,115,22,0.4)]"
          />
        </Link>
        <p className="text-xs text-slate-400 font-medium mt-3 tracking-wide">
          Exam management and proctoring portal
        </p>
      </div>

      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-100 p-6 sm:p-8 relative z-10">
        {isAuthenticated ? (
          <div className="text-center py-6">
            <div className="w-16 h-16 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Icon name="check-circle" className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-black text-slate-900">You are already signed in</h2>
            <p className="text-sm text-slate-500 mt-1 mb-6">
              Active Role: <span className="font-bold text-orange-600 capitalize">{role}</span>
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <button
                onClick={() => navigate(`/dashboard/${(role || "student").toLowerCase()}`)}
                className="py-3 px-6 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-2xl shadow-md transition-all"
              >
                Go to Dashboard
              </button>
              <button
                onClick={logout}
                className="py-3 px-6 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-2xl transition-all"
              >
                Sign Out
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="flex bg-slate-100 p-1.5 rounded-2xl">
              <button
                type="button"
                onClick={() => setActiveTab("login")}
                className={`flex-1 py-2.5 text-sm font-bold rounded-xl transition-all ${
                  activeTab === "login"
                    ? "bg-white text-orange-600 shadow-sm"
                    : "text-slate-500 hover:text-slate-900"
                }`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => setActiveTab("signup")}
                className={`flex-1 py-2.5 text-sm font-bold rounded-xl transition-all ${
                  activeTab === "signup"
                    ? "bg-white text-orange-600 shadow-sm"
                    : "text-slate-500 hover:text-slate-900"
                }`}
              >
                Register
              </button>
            </div>

            <div className="mt-5 text-center">
              <h2 className="text-2xl font-black text-slate-900">
                {activeTab === "login" ? "Welcome back" : "Create an Account"}
              </h2>
              <p className="text-xs text-slate-500 mt-1.5">
                {activeTab === "login"
                  ? "Enter your credentials to access your dashboard"
                  : "Submit a registration request for admin approval"}
              </p>
            </div>

            {activeTab === "login" ? (
              <Login />
            ) : (
              <Signup onSignupSuccess={() => setActiveTab("login")} />
            )}
          </>
        )}
      </div>

      <div className="mt-8 text-center z-10">
        <Link
          to="/"
          className="text-xs font-semibold text-slate-400 hover:text-orange-400 transition-colors flex items-center justify-center gap-1.5"
        >
          <span>←</span>
          <span>Back to Home</span>
        </Link>
      </div>
    </section>
  );
}
