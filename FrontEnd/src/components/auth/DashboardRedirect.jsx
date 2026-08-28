import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function DashboardRedirect() {
  const { role } = useAuth();
  const targetRole = (role || "student").toLowerCase();
  return <Navigate to={`/dashboard/${targetRole}`} replace />;
}
