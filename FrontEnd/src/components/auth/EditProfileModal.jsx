import React, { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import apiClient from "../../api/client";
import { Icon } from "../common/Icons";

export default function EditProfileModal({ isOpen, onClose }) {
  const { user, role, updateUser } = useAuth();
  const { showSuccess, showError } = useToast();

  const [formData, setFormData] = useState({
    name: "",
    username: "",
    email: "",
    contact: "",
    password: "",
    confirmPassword: "",
    profileImage: "",
  });

  const [initialLoading, setInitialLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (!isOpen || !user?.userId) return;

    let isMounted = true;
    const fetchLatestProfile = async () => {
      setInitialLoading(true);
      setErrors({});
      try {
        const response = await apiClient.get(`/api/auth/profile/${user.userId}`);
        const profile = response.data;
        if (isMounted && profile) {
          setFormData({
            name: profile.name || user?.name || user?.username || "",
            username: profile.username || user?.username || "",
            email: profile.email || user?.email || "",
            contact: profile.contact || user?.contact || "",
            password: "",
            confirmPassword: "",
            profileImage: profile.profile_image || profile.profileImage || user?.profileImage || "",
          });
        }
      } catch {
        if (isMounted) {
          // Fallback to existing auth state
          setFormData({
            name: user?.name || user?.username || "",
            username: user?.username || "",
            email: user?.email || "",
            contact: user?.contact || "",
            password: "",
            confirmPassword: "",
            profileImage: user?.profileImage || "",
          });
        }
      } finally {
        if (isMounted) {
          setInitialLoading(false);
        }
      }
    };

    fetchLatestProfile();

    return () => {
      isMounted = false;
    };
  }, [isOpen, user?.userId, user?.name, user?.username, user?.email, user?.contact, user?.profileImage]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const handleImageChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setErrors((prev) => ({ ...prev, profileImage: "Please select a valid image file (PNG, JPG, JPEG, WEBP)" }));
      return;
    }

    // Limit image size to 2.5 MB to keep payload and storage efficient
    if (file.size > 2.5 * 1024 * 1024) {
      setErrors((prev) => ({ ...prev, profileImage: "Image size must be under 2.5 MB" }));
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setFormData((prev) => ({ ...prev, profileImage: reader.result }));
      setErrors((prev) => ({ ...prev, profileImage: "" }));
    };
    reader.onerror = () => {
      setErrors((prev) => ({ ...prev, profileImage: "Failed to read image file" }));
    };
    reader.readAsDataURL(file);
  };

  const handleRemoveImage = () => {
    setFormData((prev) => ({ ...prev, profileImage: "" }));
  };

  const validate = () => {
    const errs = {};
    if (!formData.name.trim()) {
      errs.name = "Full Name is required";
    }
    if (!formData.username.trim()) {
      errs.username = "Username is required";
    }
    if (!formData.email.trim()) {
      errs.email = "Email Address is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errs.email = "Please enter a valid email address";
    }

    if (formData.password) {
      if (formData.password.length < 4) {
        errs.password = "New password must be at least 4 characters";
      } else if (formData.password !== formData.confirmPassword) {
        errs.confirmPassword = "Passwords do not match";
      }
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

    setSaving(true);
    setErrors({});

    const payload = {
      user_id: user.userId,
      name: formData.name.trim(),
      username: formData.username.trim(),
      email: formData.email.trim(),
      contact: formData.contact.trim(),
      password: formData.password ? formData.password.trim() : null,
      profile_image: formData.profileImage || null,
    };

    try {
      const response = await apiClient.put(`/api/auth/profile/${user.userId}`, payload);
      const updatedProfile = response.data.profile || payload;

      updateUser({
        name: updatedProfile.name,
        username: updatedProfile.username,
        email: updatedProfile.email,
        contact: updatedProfile.contact,
        profileImage: updatedProfile.profile_image || updatedProfile.profileImage || null,
      });

      showSuccess(response.data.message || "Profile updated successfully!");
      onClose();
    } catch (err) {
      const errorMsg = err.message || "Failed to update profile";
      setErrors({ general: errorMsg });
      showError(errorMsg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 sm:p-6">
      <div
        className="relative bg-white rounded-3xl shadow-2xl max-w-xl w-full overflow-hidden border border-slate-100 animate-in fade-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 sm:px-8 py-5 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
              <Icon name="edit" className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900">Edit Profile</h2>
              <p className="text-xs text-slate-500 font-medium">
                Update your account details and profile picture
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
            aria-label="Close modal"
          >
            <Icon name="x" className="w-5 h-5" />
          </button>
        </div>

        {initialLoading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-3 text-slate-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
            <span className="text-xs font-semibold">Loading profile information...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 sm:p-8 space-y-6">
            {/* General Error Banner */}
            {errors.general && (
              <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold flex items-center gap-2">
                <Icon name="alert" className="w-4 h-4 flex-shrink-0" />
                <span>{errors.general}</span>
              </div>
            )}

            {/* Profile Image Section */}
            <div className="flex flex-col sm:flex-row items-center gap-5 p-4 rounded-2xl bg-slate-50 border border-slate-100">
              <div className="relative group">
                <div className="w-20 h-20 rounded-2xl bg-orange-500 text-white flex items-center justify-center font-bold text-2xl shadow-md overflow-hidden border-2 border-white flex-shrink-0">
                  {formData.profileImage ? (
                    <img
                      src={formData.profileImage}
                      alt="Profile preview"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    (formData.name || formData.username || "U").charAt(0).toUpperCase()
                  )}
                </div>
              </div>

              <div className="flex-1 text-center sm:text-left space-y-2">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
                  <label className="cursor-pointer inline-flex items-center gap-1.5 px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-xs font-bold shadow-sm transition-all">
                    <Icon name="camera" className="w-4 h-4" />
                    <span>Upload Photo</span>
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleImageChange}
                    />
                  </label>

                  {formData.profileImage && (
                    <button
                      type="button"
                      onClick={handleRemoveImage}
                      className="inline-flex items-center gap-1 px-3 py-2 bg-slate-200 hover:bg-rose-50 hover:text-rose-600 text-slate-600 rounded-xl text-xs font-semibold transition-all"
                    >
                      <Icon name="trash" className="w-3.5 h-3.5" />
                      Remove
                    </button>
                  )}
                </div>
                <p className="text-[11px] text-slate-500">
                  Allowed formats: PNG, JPG, JPEG, WEBP. Max size: 2.5 MB.
                </p>
                {errors.profileImage && (
                  <p className="text-xs text-rose-500 font-semibold">{errors.profileImage}</p>
                )}
              </div>
            </div>

            {/* Profile Fields Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Full Name */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Full Name <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    placeholder="Enter your full name"
                    className={`w-full px-4 py-2.5 rounded-xl border text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                      errors.name
                        ? "border-rose-400 focus:ring-rose-200"
                        : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                    }`}
                  />
                </div>
                {errors.name && <p className="text-xs text-rose-500 font-medium">{errors.name}</p>}
              </div>

              {/* Username */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Username <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    placeholder="Enter your username"
                    className={`w-full px-4 py-2.5 rounded-xl border text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                      errors.username
                        ? "border-rose-400 focus:ring-rose-200"
                        : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                    }`}
                  />
                </div>
                {errors.username && (
                  <p className="text-xs text-rose-500 font-medium">{errors.username}</p>
                )}
              </div>

              {/* Email */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Email Address <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="name@domain.com"
                    className={`w-full px-4 py-2.5 rounded-xl border text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                      errors.email
                        ? "border-rose-400 focus:ring-rose-200"
                        : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                    }`}
                  />
                </div>
                {errors.email && <p className="text-xs text-rose-500 font-medium">{errors.email}</p>}
              </div>

              {/* Contact Number */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Contact Number
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="contact"
                    value={formData.contact}
                    onChange={handleChange}
                    placeholder="e.g. 9876543210"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100 transition-all"
                  />
                </div>
              </div>
            </div>

            {/* Role Badge (Read-only) */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs">
              <span className="font-semibold text-slate-500">Account Role</span>
              <span className="px-3 py-1 bg-orange-100 text-orange-700 font-bold rounded-full capitalize">
                {role || user?.role || "Student"}
              </span>
            </div>

            {/* Password Section */}
            <div className="pt-2 border-t border-slate-100 space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Change Password (Optional)
                </h3>
                <span className="text-[11px] text-slate-400 font-medium">
                  Leave empty to keep current password
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* New Password */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-600">
                    New Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      name="password"
                      value={formData.password}
                      onChange={handleChange}
                      placeholder="••••••••"
                      className={`w-full px-4 py-2.5 pr-10 rounded-xl border text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                        errors.password
                          ? "border-rose-400 focus:ring-rose-200"
                          : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
                    >
                      <Icon name={showPassword ? "eye-off" : "eye"} className="w-4 h-4" />
                    </button>
                  </div>
                  {errors.password && (
                    <p className="text-xs text-rose-500 font-medium">{errors.password}</p>
                  )}
                </div>

                {/* Confirm Password */}
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-600">
                    Confirm New Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      placeholder="••••••••"
                      className={`w-full px-4 py-2.5 rounded-xl border text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                        errors.confirmPassword
                          ? "border-rose-400 focus:ring-rose-200"
                          : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                      }`}
                    />
                  </div>
                  {errors.confirmPassword && (
                    <p className="text-xs text-rose-500 font-medium">{errors.confirmPassword}</p>
                  )}
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                disabled={saving}
                className="px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-all"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="px-6 py-2.5 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 disabled:opacity-60 text-white rounded-xl text-xs font-bold shadow-md hover:shadow-lg transition-all flex items-center gap-2"
              >
                {saving && (
                  <div className="animate-spin rounded-full h-3.5 w-3.5 border-2 border-white border-t-transparent"></div>
                )}
                <span>{saving ? "Saving Changes..." : "Save Changes"}</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
