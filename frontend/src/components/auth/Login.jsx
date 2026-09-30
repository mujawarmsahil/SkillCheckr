import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function Login() {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validate = () => {
    const errs = {};
    if (!formData.username.trim()) {
      errs.username = "Username is required";
    }
    if (!formData.password) {
      errs.password = "Password is required";
    }
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }

    setLoading(true);
    try {
      const data = await login(formData);
      showSuccess(`Signed in as ${data.role}.`);

      const userRole = (data.role || "student").toLowerCase();
      const redirectPath = location.state?.from?.pathname || `/dashboard/${userRole}`;
      navigate(redirectPath, { replace: true });
    } catch (err) {
      showError(err.message || "Invalid username or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-5">
      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Username
        </label>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Icon name="user" className="w-5 h-5" />
          </div>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            placeholder="Enter your username"
            className={`w-full pl-11 pr-4 py-2.5 bg-slate-50 border ${
              errors.username ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
            } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
          />
        </div>
        {errors.username && <p className="mt-1 text-xs text-rose-600">{errors.username}</p>}
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Password
        </label>
        <div className="relative">
          <input
            type={showPassword ? "text" : "password"}
            name="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="Enter your password"
            className={`w-full pl-4 pr-11 py-2.5 bg-slate-50 border ${
              errors.password ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
            } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600"
          >
            <Icon name={showPassword ? "eye-off" : "eye"} className="w-5 h-5" />
          </button>
        </div>
        {errors.password && <p className="mt-1 text-xs text-rose-600">{errors.password}</p>}
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full mt-2 py-3 px-4 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {loading ? (
          <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
        ) : (
          "Sign In"
        )}
      </button>
    </form>
  );
}
