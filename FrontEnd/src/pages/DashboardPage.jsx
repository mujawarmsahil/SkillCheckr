import React, { useState } from "react";
import { useParams, useNavigate, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AdminDashboard from "../components/admin/AdminDashboard";
import TeacherDashboard from "../components/teacher/TeacherDashboard";
import StudentDashboard from "../components/student/StudentDashboard";
import EditProfileModal from "../components/auth/EditProfileModal";
import { Icon } from "../components/common/Icons";

export default function DashboardPage() {
  const { role: urlRole } = useParams();
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);

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
        <div className="w-full max-w-[1536px] mx-auto px-4 sm:px-8 lg:px-12 py-4 sm:py-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3 sm:gap-4 min-w-0">
              <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-orange-500 text-white flex items-center justify-center font-bold text-lg sm:text-xl shadow-sm overflow-hidden border-2 border-orange-100 shrink-0">
                {user?.profileImage ? (
                  <img
                    src={user.profileImage}
                    alt={user.name || user.username || "Profile"}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  (user?.name || user?.username || "U").charAt(0).toUpperCase()
                )}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2 sm:gap-2.5">
                  <h1 className="text-lg sm:text-xl font-black text-slate-900 truncate">
                    Welcome, {user?.name || user?.username || "User"}
                  </h1>
                  <span className="text-[11px] sm:text-xs font-bold px-2.5 sm:px-3 py-0.5 sm:py-1 rounded-full bg-orange-100 text-orange-700 capitalize shrink-0">
                    {role || activeRole}
                  </span>
                </div>
                <p className="text-[11px] sm:text-xs text-slate-500 mt-0.5 font-medium truncate">
                  {user?.email ? `${user.email} • ` : ""}@{user?.username || "user"} • SkillCheckr Academic Portal
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 sm:gap-2.5 w-full sm:w-auto">
              <button
                onClick={() => setIsEditProfileOpen(true)}
                className="flex-1 sm:flex-initial py-2.5 px-4 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 shadow-sm hover:shadow"
              >
                <Icon name="edit" className="w-4 h-4" />
                <span>Edit Profile</span>
              </button>
              <button
                onClick={handleLogout}
                className="flex-1 sm:flex-initial py-2.5 px-4 bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-600 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5"
              >
                <Icon name="logout" className="w-4 h-4" />
                <span>Sign Out</span>
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

      {/* Edit Profile Modal */}
      <EditProfileModal
        isOpen={isEditProfileOpen}
        onClose={() => setIsEditProfileOpen(false)}
      />
    </div>
  );
}
