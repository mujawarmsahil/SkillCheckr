import React from "react";
import { useParams, useNavigate, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AdminDashboard from "../components/admin/AdminDashboard";
import TeacherDashboard from "../components/teacher/TeacherDashboard";
import StudentDashboard from "../components/student/StudentDashboard";
import { Icon } from "../components/common/Icons";

export default function DashboardPage() {
  const { role: urlRole } = useParams();
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const activeRole = (urlRole || role || "student").toLowerCase();

  // If user tries to access a dashboard they don't have permission for, redirect to their role
  if (role && role.toLowerCase() !== activeRole) {
    return <Navigate to={`/dashboard/${role.toLowerCase()}`} replace />;
  }

  const handleLogout = () => {
    logout();
    navigate("/authentication");
  };

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Dashboard Sub-Header / Welcome Banner */}
      <div className="bg-white border-b border-slate-200">
        <div className="w-full max-w-[1536px] mx-auto px-4 sm:px-8 lg:px-12 py-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-orange-500 text-white flex items-center justify-center font-bold text-lg shadow-sm">
                {(user?.username || "U").charAt(0).toUpperCase()}
              </div>
              <div>
                <div className="flex items-center gap-2.5">
                  <h1 className="text-xl font-black text-slate-900">
                    Welcome, {user?.username || "User"}
                  </h1>
                  <span className="text-xs font-bold px-3 py-1 rounded-full bg-orange-100 text-orange-700 capitalize">
                    {role || activeRole}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5 font-medium">
                  SkillCheckr Academic Assessment & Evaluation Portal
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleLogout}
                className="py-2.5 px-4 bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-600 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5"
              >
                <Icon name="logout" className="w-4 h-4" />
                Sign Out
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Dashboard Content Area */}
      <div className="w-full max-w-[1536px] mx-auto px-4 sm:px-8 lg:px-12 py-8">
        {activeRole === "admin" && <AdminDashboard />}
        {activeRole === "teacher" && <TeacherDashboard />}
        {activeRole === "student" && <StudentDashboard />}
      </div>
    </div>
  );
}
