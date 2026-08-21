import React, { createContext, useContext, useState, useEffect } from "react";
import apiClient from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const storedRole = localStorage.getItem("role");
      const storedUsername = localStorage.getItem("username");
      const storedUserId = localStorage.getItem("user_id");
      const storedToken = localStorage.getItem("token");
      const storedTeacherId = localStorage.getItem("teacher_id");
      const storedStudentId = localStorage.getItem("student_id");

      if (storedRole && storedUserId) {
        const roleId = storedRole === "Teacher" ? storedTeacherId : storedRole === "Student" ? storedStudentId : null;
        setUser({
          username: storedUsername || "User",
          role: storedRole,
          userId: parseInt(storedUserId, 10),
          roleId: roleId ? parseInt(roleId, 10) : null,
        });
        setRole(storedRole);
        setToken(storedToken);
      }
    } catch (e) {
      console.error("Failed to load auth session", e);
    } finally {
      setLoading(false);
    }
  }, []);

  const login = async ({ username, password }) => {
    const response = await apiClient.post("/api/authentication/login", {
      username: username.trim(),
      password,
    });

    const data = response.data;
    const userRole = data.role;
    const userId = data.userId || data.user_id;
    const roleId = data.roleId || data.role_id;
    const userToken = data.token || "session-token-" + userId;
    const resolvedUsername = data.username || username.trim();

    localStorage.setItem("username", resolvedUsername);
    localStorage.setItem("role", userRole);
    localStorage.setItem("user_id", userId);
    localStorage.setItem("token", userToken);

    if (userRole === "Teacher" && roleId) {
      localStorage.setItem("teacher_id", roleId);
    } else if (userRole === "Student" && roleId) {
      localStorage.setItem("student_id", roleId);
    }

    setUser({
      username: resolvedUsername,
      role: userRole,
      userId,
      roleId,
    });
    setRole(userRole);
    setToken(userToken);

    return data;
  };

  const logout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    localStorage.removeItem("user_id");
    localStorage.removeItem("teacher_id");
    localStorage.removeItem("student_id");
    localStorage.removeItem("token");
    localStorage.removeItem("subject_id");
    localStorage.removeItem("examName");

    setUser(null);
    setRole(null);
    setToken(null);
  };

  const hasRole = (allowedRoles) => {
    if (!role) return false;
    if (typeof allowedRoles === "string") {
      return role.toLowerCase() === allowedRoles.toLowerCase();
    }
    return allowedRoles.some((r) => r.toLowerCase() === role.toLowerCase());
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        token,
        loading,
        isAuthenticated: !!user,
        login,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
