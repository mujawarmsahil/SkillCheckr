import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/authentication" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const isAllowed = allowedRoles.some(
      (r) => r.toLowerCase() === (role || "").toLowerCase()
    );
    if (!isAllowed) {
      // Redirect to their own dashboard
      const targetRole = (role || "student").toLowerCase();
      return <Navigate to={`/dashboard/${targetRole}`} replace />;
    }
  }

  return children;
}
