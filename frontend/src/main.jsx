import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
  RouterProvider,
  Navigate,
} from "react-router-dom";
import "./index.css";

import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";

import App from "./App";
import Home from "./components/public/Home";
import About from "./components/public/About";
import Blog from "./components/public/Blog";
import Contact from "./components/public/Contact";
import Authentication from "./components/auth/Authentication";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import DashboardRedirect from "./components/auth/DashboardRedirect";
import DashboardPage from "./pages/DashboardPage";
import TakeExam from "./components/student/TakeExam";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      {/* Public Pages with Layout Header & Footer */}
      <Route path="/" element={<App />}>
        <Route index element={<Home />} />
        <Route path="about" element={<About />} />
        <Route path="blog" element={<Blog />} />
        <Route path="contact" element={<Contact />} />
      </Route>

      <Route path="/authentication" element={<Authentication />} />
      <Route path="/login" element={<Navigate to="/authentication" replace />} />
      <Route path="/signup" element={<Navigate to="/authentication" replace />} />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardRedirect />
          </ProtectedRoute>
        }
      />
      <Route
        path="/dashboard/:role"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/user/:role"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/take-exam/:examId"
        element={
          <ProtectedRoute allowedRoles={["Student", "Admin", "Teacher"]}>
            <TakeExam />
          </ProtectedRoute>
        }
      />
      <Route
        path="/studentExams"
        element={<Navigate to="/dashboard/student" replace />}
      />
      <Route
        path="/createExam"
        element={<Navigate to="/dashboard/teacher" replace />}
      />
      <Route
        path="/question"
        element={<Navigate to="/dashboard/teacher" replace />}
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </>
  )
);

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <AuthProvider>
      <ToastProvider>
        <RouterProvider router={router} />
      </ToastProvider>
    </AuthProvider>
  </StrictMode>
);
