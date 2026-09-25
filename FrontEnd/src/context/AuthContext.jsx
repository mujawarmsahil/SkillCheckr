import React, { createContext, useContext, useState, useEffect } from "react";
import apiClient from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const storedRole = localStorage.getItem("role");
      const storedUsername = localStorage.getItem("username");
      const storedName = localStorage.getItem("name");
      const storedEmail = localStorage.getItem("email");
      const storedProfileImage = localStorage.getItem("profile_image");
      const storedContact = localStorage.getItem("contact");
      const storedUserId = localStorage.getItem("user_id");
      const storedTeacherId = localStorage.getItem("teacher_id");
      const storedStudentId = localStorage.getItem("student_id");

      if (storedRole && storedUserId) {
        const roleId = storedRole === "Teacher" ? storedTeacherId : storedRole === "Student" ? storedStudentId : null;
        setUser({
          username: storedUsername || "User",
          name: storedName || storedUsername || "User",
          email: storedEmail || "",
          profileImage: storedProfileImage || null,
          contact: storedContact || "",
          role: storedRole,
          userId: parseInt(storedUserId, 10),
          roleId: roleId ? parseInt(roleId, 10) : null,
        });
        setRole(storedRole);
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
    const resolvedName = data.name || resolvedUsername;
    const resolvedEmail = data.email || data.student_email || data.teacher_email || data.admin_email || "";
    const resolvedContact = data.contact || data.student_contact || data.teacher_contact || data.admin_contact || "";
    const resolvedProfileImage = data.profile_image || data.profileImage || null;

    localStorage.setItem("username", resolvedUsername);
    localStorage.setItem("name", resolvedName);
    localStorage.setItem("role", userRole);
    localStorage.setItem("user_id", userId);
    localStorage.setItem("token", userToken);
    if (resolvedEmail) localStorage.setItem("email", resolvedEmail);
    if (resolvedContact) localStorage.setItem("contact", resolvedContact);
    if (resolvedProfileImage) {
      localStorage.setItem("profile_image", resolvedProfileImage);
    } else {
      localStorage.removeItem("profile_image");
    }

    if (userRole === "Teacher" && roleId) {
      localStorage.setItem("teacher_id", roleId);
    } else if (userRole === "Student" && roleId) {
      localStorage.setItem("student_id", roleId);
    }

    setUser({
      username: resolvedUsername,
      name: resolvedName,
      email: resolvedEmail,
      contact: resolvedContact,
      profileImage: resolvedProfileImage,
      role: userRole,
      userId,
      user_id: userId,
      roleId,
      role_id: roleId,
    });
    setRole(userRole);

    return data;
  };

  const updateUser = (updatedData) => {
    if (!updatedData) return;

    if (updatedData.username) {
      localStorage.setItem("username", updatedData.username);
    }
    if (updatedData.name) {
      localStorage.setItem("name", updatedData.name);
    }
    if (updatedData.email !== undefined) {
      localStorage.setItem("email", updatedData.email);
    }
    if (updatedData.contact !== undefined) {
      localStorage.setItem("contact", updatedData.contact);
    }

    const newProfileImage =
      updatedData.profile_image !== undefined
        ? updatedData.profile_image
        : updatedData.profileImage;

    if (newProfileImage !== undefined) {
      if (newProfileImage) {
        localStorage.setItem("profile_image", newProfileImage);
      } else {
        localStorage.removeItem("profile_image");
      }
    }

    setUser((prev) => {
      if (!prev) return null;
      return {
        ...prev,
        username: updatedData.username || prev.username,
        name: updatedData.name || prev.name,
        email: updatedData.email !== undefined ? updatedData.email : prev.email,
        contact: updatedData.contact !== undefined ? updatedData.contact : prev.contact,
        profileImage:
          newProfileImage !== undefined ? newProfileImage : prev.profileImage,
      };
    });
  };

  const setAuthSession = (data) => {
    if (!data) return;

    const userRole = data.role || "Student";
    const userId = data.userId || data.user_id;
    const roleId = data.roleId || data.role_id;
    const userToken = data.token || "session-token-" + userId;
    const resolvedUsername = data.username || "User";
    const resolvedName = data.name || resolvedUsername;
    const resolvedEmail = data.email || "";
    const resolvedProfileImage = data.profile_image || data.profileImage || null;
    const resolvedContact = data.contact || "";

    localStorage.setItem("username", resolvedUsername);
    localStorage.setItem("name", resolvedName);
    localStorage.setItem("role", userRole);
    if (userId) localStorage.setItem("user_id", userId);
    if (userToken) localStorage.setItem("token", userToken);
    if (resolvedEmail) localStorage.setItem("email", resolvedEmail);
    if (resolvedContact) localStorage.setItem("contact", resolvedContact);
    if (resolvedProfileImage) {
      localStorage.setItem("profile_image", resolvedProfileImage);
    } else {
      localStorage.removeItem("profile_image");
    }

    if (userRole === "Teacher" && roleId) {
      localStorage.setItem("teacher_id", roleId);
    } else if (userRole === "Student" && roleId) {
      localStorage.setItem("student_id", roleId);
    }

    setUser({
      username: resolvedUsername,
      name: resolvedName,
      email: resolvedEmail,
      contact: resolvedContact,
      profileImage: resolvedProfileImage,
      role: userRole,
      userId: userId ? parseInt(userId, 10) : null,
      roleId: roleId ? parseInt(roleId, 10) : null,
    });
    setRole(userRole);
  };

  const logout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("name");
    localStorage.removeItem("email");
    localStorage.removeItem("contact");
    localStorage.removeItem("profile_image");
    localStorage.removeItem("role");
    localStorage.removeItem("user_id");
    localStorage.removeItem("teacher_id");
    localStorage.removeItem("student_id");
    localStorage.removeItem("token");
    localStorage.removeItem("subject_id");
    localStorage.removeItem("examName");

    setUser(null);
    setRole(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        loading,
        isAuthenticated: !!user,
        login,
        logout,
        updateUser,
        setAuthSession,
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
