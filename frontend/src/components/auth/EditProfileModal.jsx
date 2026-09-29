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
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
    profileImage: "",
  });

  const [changePassword, setChangePassword] = useState(false);
  const [initialLoading, setInitialLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    const currentUserId = user?.userId || user?.user_id || localStorage.getItem("user_id");
    if (!isOpen || !currentUserId) return;

    let isMounted = true;
    setChangePassword(false);
    setShowOldPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);

    const fallbackEmail =
      user?.email ||
      user?.student_email ||
      user?.teacher_email ||
      user?.admin_email ||
      localStorage.getItem("email") ||
      "";
    const fallbackName =
      user?.name ||
      user?.student_name ||
      user?.teacher_name ||
      user?.admin_name ||
      user?.username ||
      localStorage.getItem("name") ||
      localStorage.getItem("username") ||
      "";
    const fallbackUsername =
      user?.username ||
      localStorage.getItem("username") ||
      "";
    const fallbackContact =
      user?.contact ||
      user?.student_contact ||
      user?.teacher_contact ||
      user?.admin_contact ||
      localStorage.getItem("contact") ||
      "";
    const fallbackImage =
      user?.profileImage ||
      user?.profile_image ||
      localStorage.getItem("profile_image") ||
      "";

    // Fill from local state first so the form is never blank
    setFormData({
      name: fallbackName,
      username: fallbackUsername,
      email: fallbackEmail,
      contact: fallbackContact,
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
      profileImage: fallbackImage,
    });

    const fetchLatestProfile = async () => {
      setInitialLoading(true);
      setErrors({});
      try {
        const response = await apiClient.get(`/api/auth/profile/${currentUserId}`);
        const profile = response.data;
        if (isMounted && profile) {
          const resolvedEmail =
            profile.email ||
            profile.student_email ||
            profile.teacher_email ||
            profile.admin_email ||
            fallbackEmail;
          const resolvedName =
            profile.name ||
            profile.student_name ||
            profile.teacher_name ||
            profile.admin_name ||
            fallbackName;
          const resolvedUsername = profile.username || fallbackUsername;
          const resolvedContact =
            profile.contact ||
            profile.student_contact ||
            profile.teacher_contact ||
            profile.admin_contact ||
            fallbackContact;
          const resolvedImage =
            profile.profile_image ||
            profile.profileImage ||
            fallbackImage;

          setFormData({
            name: resolvedName,
            username: resolvedUsername,
            email: resolvedEmail,
            contact: resolvedContact,
            oldPassword: "",
            newPassword: "",
            confirmPassword: "",
            profileImage: resolvedImage,
          });

          if (resolvedEmail) {
            localStorage.setItem("email", resolvedEmail);
          }
          if (resolvedContact) {
            localStorage.setItem("contact", resolvedContact);
          }
        }
      } catch {
        // Keep the pre-filled values if the fetch fails
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
  }, [isOpen, user]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const handlePasswordToggle = (e) => {
    const checked = e.target.checked;
    setChangePassword(checked);
    setFormData((prev) => ({
      ...prev,
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    }));
    setErrors((prev) => {
      const updated = { ...prev };
      delete updated.oldPassword;
      delete updated.newPassword;
      delete updated.confirmPassword;
      return updated;
    });
  };

  const handleImageChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setErrors((prev) => ({ ...prev, profileImage: "Select a valid image file (PNG, JPG, JPEG, WEBP)" }));
      return;
    }

    // 2.5 MB cap keeps the base64 payload manageable
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
      errs.name = "Full name is required";
    }
    if (!formData.username.trim()) {
      errs.username = "Username is required";
    }
    if (!formData.email.trim()) {
      errs.email = "Email address is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errs.email = "Enter a valid email address";
    }

    if (changePassword) {
      if (!formData.oldPassword) {
        errs.oldPassword = "Current password is required";
      }
      if (!formData.newPassword) {
        errs.newPassword = "New password is required";
      } else if (formData.newPassword.length < 4) {
        errs.newPassword = "New password must be at least 4 characters";
      }
      if (!formData.confirmPassword) {
        errs.confirmPassword = "Confirm password is required";
      } else if (formData.newPassword && formData.newPassword !== formData.confirmPassword) {
        errs.confirmPassword = "Passwords do not match";
      }
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

    setSaving(true);
    setErrors({});

    const currentUserId = user?.userId || user?.user_id || localStorage.getItem("user_id");
    const payload = {
      user_id: currentUserId,
      name: formData.name.trim(),
      username: formData.username.trim(),
      email: formData.email.trim(),
      contact: formData.contact.trim(),
      password: changePassword && formData.newPassword ? formData.newPassword.trim() : null,
      current_password: changePassword && formData.oldPassword ? formData.oldPassword.trim() : null,
      old_password: changePassword && formData.oldPassword ? formData.oldPassword.trim() : null,
      profile_image: formData.profileImage || null,
    };

    try {
      const response = await apiClient.put(`/api/auth/profile/${currentUserId}`, payload);
      const updatedProfile = response.data.profile || payload;

      updateUser({
        name: updatedProfile.name,
        username: updatedProfile.username,
        email: updatedProfile.email,
        contact: updatedProfile.contact,
        profileImage: updatedProfile.profile_image || updatedProfile.profileImage || null,
      });

      if (updatedProfile.email) {
        localStorage.setItem("email", updatedProfile.email);
      }
      if (updatedProfile.contact) {
        localStorage.setItem("contact", updatedProfile.contact);
      }

      showSuccess(response.data.message || "Profile updated.");
      onClose();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to update profile";
      if (errorMsg.toLowerCase().includes("current password") || errorMsg.toLowerCase().includes("old password")) {
        setErrors({ oldPassword: errorMsg, general: errorMsg });
      } else {
        setErrors({ general: errorMsg });
      }
      showError(errorMsg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-5 md:p-6 overscroll-contain">
      <div
        className="relative bg-white rounded-2xl sm:rounded-3xl shadow-2xl max-w-xl w-full max-h-[92vh] sm:max-h-[90vh] flex flex-col overflow-hidden border border-slate-100 animate-in fade-in zoom-in-95 duration-150 my-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 sm:px-6 md:px-8 py-3.5 sm:py-5 border-b border-slate-100 bg-slate-50/80 shrink-0">
          <div className="flex items-center gap-3 min-w-0 mr-2">
            <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
              <Icon name="edit" className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
            <div className="min-w-0 flex-1">
              <h2 className="text-base sm:text-lg font-bold text-slate-900 truncate">Edit Profile</h2>
              <p className="text-[11px] sm:text-xs text-slate-500 font-medium truncate">
                Update your account details and profile picture
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 sm:p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-colors shrink-0"
            aria-label="Close modal"
          >
            <Icon name="x" className="w-5 h-5" />
          </button>
        </div>

        {initialLoading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-3 text-slate-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
            <span className="text-xs font-semibold">Loading profile...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} autoComplete="off" className="flex flex-col flex-1 min-h-0 overflow-hidden">
            <div className="flex-1 overflow-y-auto p-4 sm:p-6 md:p-8 space-y-4 sm:space-y-6 overscroll-contain">
              {errors.general && (
                <div className="p-3 sm:p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold flex items-center gap-2">
                  <Icon name="alert" className="w-4 h-4 shrink-0" />
                  <span className="break-words">{errors.general}</span>
                </div>
              )}

              <div className="flex flex-col sm:flex-row items-center sm:items-start md:items-center gap-4 sm:gap-5 p-3.5 sm:p-4 rounded-2xl bg-slate-50 border border-slate-100">
                <div className="relative group shrink-0">
                  <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-orange-500 text-white flex items-center justify-center font-bold text-xl sm:text-2xl shadow-md overflow-hidden border-2 border-white shrink-0">
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

                <div className="flex-1 text-center sm:text-left space-y-2 w-full sm:w-auto">
                  <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 sm:gap-2.5">
                    <label className="cursor-pointer inline-flex items-center justify-center gap-1.5 px-3.5 sm:px-4 py-2 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-sm transition-all flex-1 sm:flex-initial">
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
                        className="inline-flex items-center justify-center gap-1 px-3 py-2 bg-slate-200 hover:bg-rose-50 hover:text-rose-600 text-slate-600 rounded-xl text-xs font-semibold transition-all flex-1 sm:flex-initial"
                      >
                        <Icon name="trash" className="w-3.5 h-3.5" />
                        <span>Remove</span>
                      </button>
                    )}
                  </div>
                  <p className="text-[10px] sm:text-[11px] text-slate-500 leading-tight">
                    Allowed formats: PNG, JPG, JPEG, WEBP. Max size: 2.5 MB.
                  </p>
                  {errors.profileImage && (
                    <p className="text-xs text-rose-500 font-semibold">{errors.profileImage}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                <div className="space-y-1 sm:space-y-1.5">
                  <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Full Name <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      placeholder="Enter your full name"
                      className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                        errors.name
                          ? "border-rose-400 focus:ring-rose-200"
                          : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                      }`}
                    />
                  </div>
                  {errors.name && <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.name}</p>}
                </div>

                <div className="space-y-1 sm:space-y-1.5">
                  <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Username <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      name="username"
                      value={formData.username}
                      onChange={handleChange}
                      placeholder="Enter your username"
                      className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                        errors.username
                          ? "border-rose-400 focus:ring-rose-200"
                          : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                      }`}
                    />
                  </div>
                  {errors.username && (
                    <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.username}</p>
                  )}
                </div>

                <div className="space-y-1 sm:space-y-1.5">
                  <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Email Address <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      placeholder="name@domain.com"
                      className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                        errors.email
                          ? "border-rose-400 focus:ring-rose-200"
                          : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                      }`}
                    />
                  </div>
                  {errors.email && <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.email}</p>}
                </div>

                <div className="space-y-1 sm:space-y-1.5">
                  <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Contact Number
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      name="contact"
                      value={formData.contact}
                      onChange={handleChange}
                      placeholder="e.g. 9876543210"
                      className="w-full px-3.5 sm:px-4 py-2 sm:py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100 transition-all"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between p-2.5 sm:p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs">
                <span className="font-semibold text-slate-500 text-[11px] sm:text-xs">Account Role</span>
                <span className="px-2.5 sm:px-3 py-0.5 sm:py-1 bg-orange-100 text-orange-700 font-bold rounded-full capitalize text-[11px] sm:text-xs">
                  {role || user?.role || "Student"}
                </span>
              </div>

              <div className="pt-2 border-t border-slate-100 space-y-3 sm:space-y-4">
                <label
                  className={`group flex items-center justify-between p-3 sm:p-4 rounded-2xl border transition-all duration-200 cursor-pointer select-none ${
                    changePassword
                      ? "bg-orange-50/70 border-orange-300 shadow-sm ring-1 ring-orange-400/20"
                      : "bg-slate-50/80 border-slate-200/80 hover:bg-slate-100/70 hover:border-slate-300"
                  }`}
                >
                  <div className="flex items-center gap-3 sm:gap-3.5 min-w-0">
                    <div className="relative flex items-center justify-center shrink-0">
                      <input
                        type="checkbox"
                        checked={changePassword}
                        onChange={handlePasswordToggle}
                        className="sr-only peer"
                      />
                      <div
                        className={`w-5 h-5 rounded-lg border-2 flex items-center justify-center transition-all duration-200 shrink-0 ${
                          changePassword
                            ? "bg-orange-500 border-orange-500 text-white shadow-sm shadow-orange-500/25"
                            : "bg-white border-slate-300 group-hover:border-orange-300"
                        } peer-focus-visible:ring-2 peer-focus-visible:ring-orange-500 peer-focus-visible:ring-offset-2`}
                      >
                        {changePassword && (
                          <Icon name="check" className="w-3.5 h-3.5 text-white" />
                        )}
                      </div>
                    </div>
                    <div className="min-w-0">
                      <span
                        className={`text-[11px] sm:text-xs font-bold uppercase tracking-wider block transition-colors truncate ${
                          changePassword ? "text-orange-950" : "text-slate-800"
                        }`}
                      >
                        Change Password
                      </span>
                      <span className="text-[10px] sm:text-[11px] text-slate-500 font-normal leading-tight block">
                        Check this if you want to update your account password
                      </span>
                    </div>
                  </div>

                  {changePassword ? (
                    <span className="inline-flex items-center text-[10px] sm:text-[11px] font-bold px-2 sm:px-2.5 py-0.5 sm:py-1 rounded-lg bg-orange-100 text-orange-700 shrink-0 ml-2">
                      Active
                    </span>
                  ) : (
                    <span className="inline-flex items-center text-[10px] sm:text-[11px] font-semibold text-slate-400 shrink-0 ml-2">
                      Optional
                    </span>
                  )}
                </label>

                {changePassword && (
                  <div className="space-y-3.5 sm:space-y-4 p-3.5 sm:p-4 rounded-2xl bg-orange-50/40 border border-orange-100 animate-in fade-in slide-in-from-top-2 duration-150">
                    <div className="space-y-1 sm:space-y-1.5">
                      <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                        Current Password <span className="text-rose-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type={showOldPassword ? "text" : "password"}
                          name="oldPassword"
                          value={formData.oldPassword}
                          onChange={handleChange}
                          autoComplete="current-password"
                          placeholder="Enter current password"
                          className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 pr-10 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                            errors.oldPassword
                              ? "border-rose-400 focus:ring-rose-200"
                              : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                          }`}
                        />
                        <button
                          type="button"
                          onClick={() => setShowOldPassword(!showOldPassword)}
                          className="absolute right-2.5 sm:right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
                        >
                          <Icon name={showOldPassword ? "eye-off" : "eye"} className="w-4 h-4" />
                        </button>
                      </div>
                      {errors.oldPassword && (
                        <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.oldPassword}</p>
                      )}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                      <div className="space-y-1 sm:space-y-1.5">
                        <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                          New Password <span className="text-rose-500">*</span>
                        </label>
                        <div className="relative">
                          <input
                            type={showNewPassword ? "text" : "password"}
                            name="newPassword"
                            value={formData.newPassword}
                            onChange={handleChange}
                            autoComplete="new-password"
                            placeholder="Enter new password"
                            className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 pr-10 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                              errors.newPassword
                                ? "border-rose-400 focus:ring-rose-200"
                                : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                            }`}
                          />
                          <button
                            type="button"
                            onClick={() => setShowNewPassword(!showNewPassword)}
                            className="absolute right-2.5 sm:right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
                          >
                            <Icon name={showNewPassword ? "eye-off" : "eye"} className="w-4 h-4" />
                          </button>
                        </div>
                        {errors.newPassword && (
                          <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.newPassword}</p>
                        )}
                      </div>

                      <div className="space-y-1 sm:space-y-1.5">
                        <label className="block text-[11px] sm:text-xs font-bold text-slate-700 uppercase tracking-wider">
                          Confirm New Password <span className="text-rose-500">*</span>
                        </label>
                        <div className="relative">
                          <input
                            type={showConfirmPassword ? "text" : "password"}
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            autoComplete="new-password"
                            placeholder="Re-enter new password"
                            className={`w-full px-3.5 sm:px-4 py-2 sm:py-2.5 pr-10 rounded-xl border text-xs sm:text-sm text-slate-800 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 transition-all ${
                              errors.confirmPassword
                                ? "border-rose-400 focus:ring-rose-200"
                                : "border-slate-200 focus:border-orange-500 focus:ring-orange-100"
                            }`}
                          />
                          <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="absolute right-2.5 sm:right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
                          >
                            <Icon name={showConfirmPassword ? "eye-off" : "eye"} className="w-4 h-4" />
                          </button>
                        </div>
                        {errors.confirmPassword && (
                          <p className="text-[11px] sm:text-xs text-rose-500 font-medium">{errors.confirmPassword}</p>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="shrink-0 flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-end gap-2 sm:gap-3 px-4 sm:px-6 md:px-8 py-3.5 sm:py-4 bg-slate-50/80 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                disabled={saving}
                className="w-full sm:w-auto px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-800 hover:bg-slate-200/60 rounded-xl transition-all text-center justify-center flex items-center"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="w-full sm:w-auto px-6 py-2.5 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 disabled:opacity-60 text-white rounded-xl text-xs font-bold shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2"
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
