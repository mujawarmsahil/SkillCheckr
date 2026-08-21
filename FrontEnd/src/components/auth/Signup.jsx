import React, { useState } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function Signup({ onSignupSuccess }) {
  const [formData, setFormData] = useState({
    name: "",
    contact: "",
    email: "",
    requested_role: "Student",
    username: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const { showSuccess, showError } = useToast();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validate = () => {
    const errs = {};
    if (!formData.name.trim()) errs.name = "Full name is required";
    if (!formData.email.trim()) {
      errs.email = "Email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errs.email = "Please enter a valid email address";
    }
    if (!formData.contact.trim()) {
      errs.contact = "Contact number is required";
    } else if (!/^\d{10}$/.test(formData.contact.trim())) {
      errs.contact = "Please enter a valid 10-digit phone number";
    }
    if (!formData.username.trim()) {
      errs.username = "Username is required";
    } else if (formData.username.length < 3) {
      errs.username = "Username must be at least 3 characters";
    }
    if (!formData.password) {
      errs.password = "Password is required";
    } else if (formData.password.length < 6) {
      errs.password = "Password must be at least 6 characters";
    }
    if (!formData.requested_role || formData.requested_role === "select") {
      errs.requested_role = "Please select a role";
    }
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    try {
      await apiClient.post("/api/requests/save", formData);
      showSuccess("Registration request submitted! An Admin will review and approve your account.");
      setFormData({
        name: "",
        contact: "",
        email: "",
        requested_role: "Student",
        username: "",
        password: "",
      });
      if (onSignupSuccess) {
        onSignupSuccess();
      }
    } catch (err) {
      showError(err.message || "Failed to submit registration request. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-4">
      {/* Full Name */}
      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
          Full Name
        </label>
        <input
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          placeholder="e.g. John Doe"
          className={`w-full px-3.5 py-2 bg-slate-50 border ${
            errors.name ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
          } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
        />
        {errors.name && <p className="mt-1 text-xs text-rose-600">{errors.name}</p>}
      </div>

      {/* Email & Contact in 2 columns */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Email
          </label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="john@example.com"
            className={`w-full px-3.5 py-2 bg-slate-50 border ${
              errors.email ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
            } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
          />
          {errors.email && <p className="mt-1 text-xs text-rose-600">{errors.email}</p>}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Contact Number
          </label>
          <input
            type="tel"
            name="contact"
            value={formData.contact}
            onChange={handleChange}
            placeholder="10-digit number"
            className={`w-full px-3.5 py-2 bg-slate-50 border ${
              errors.contact ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
            } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
          />
          {errors.contact && <p className="mt-1 text-xs text-rose-600">{errors.contact}</p>}
        </div>
      </div>

      {/* Role Selection */}
      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
          Select Role
        </label>
        <div className="grid grid-cols-2 gap-3">
          {["Student", "Teacher"].map((roleOption) => (
            <button
              key={roleOption}
              type="button"
              onClick={() => setFormData((prev) => ({ ...prev, requested_role: roleOption }))}
              className={`py-2 px-3 rounded-xl border text-sm font-medium transition-all flex items-center justify-center gap-2 ${
                formData.requested_role === roleOption
                  ? "bg-orange-50 border-orange-500 text-orange-600 shadow-sm"
                  : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Icon name={roleOption === "Student" ? "book" : "users"} className="w-4 h-4" />
              {roleOption}
            </button>
          ))}
        </div>
      </div>

      {/* Username & Password */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Username
          </label>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            placeholder="Choose username"
            className={`w-full px-3.5 py-2 bg-slate-50 border ${
              errors.username ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
            } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
          />
          {errors.username && <p className="mt-1 text-xs text-rose-600">{errors.username}</p>}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Password
          </label>
          <div className="relative">
            <input
              type={showPassword ? "text" : "password"}
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="At least 6 chars"
              className={`w-full pl-3.5 pr-10 py-2 bg-slate-50 border ${
                errors.password ? "border-rose-400 ring-1 ring-rose-300" : "border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200"
              } rounded-xl text-slate-800 placeholder-slate-400 text-sm outline-none transition-all`}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
            >
              <Icon name={showPassword ? "eye-off" : "eye"} className="w-4 h-4" />
            </button>
          </div>
          {errors.password && <p className="mt-1 text-xs text-rose-600">{errors.password}</p>}
        </div>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="w-full mt-2 py-3 px-4 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {loading ? (
          <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
        ) : (
          "Submit Registration Request"
        )}
      </button>
    </form>
  );
}
