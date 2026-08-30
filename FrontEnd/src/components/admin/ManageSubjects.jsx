import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function ManageSubjects() {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState(null);
  const [formData, setFormData] = useState({ subjectName: "", subjectCode: "" });
  const [submitting, setSubmitting] = useState(false);

  const { showSuccess, showError } = useToast();

  const fetchSubjects = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/subjects/with-stats");
      setSubjects(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      showError(err.message || "Failed to load subjects");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchSubjects();
  }, [fetchSubjects]);

  const handleOpenAddModal = () => {
    setEditingSubject(null);
    setFormData({ subjectName: "", subjectCode: "" });
    setModalOpen(true);
  };

  const handleOpenEditModal = (sub) => {
    setEditingSubject(sub);
    setFormData({
      subjectName: sub.subjectName || sub.subject_name || "",
      subjectCode: sub.subjectCode || sub.subject_code || "",
    });
    setModalOpen(true);
  };

  const handleSaveSubject = async (e) => {
    e.preventDefault();
    if (!formData.subjectName.trim()) {
      showError("Please enter a subject name");
      return;
    }

    setSubmitting(true);
    try {
      if (editingSubject) {
        const id = editingSubject.subjectId || editingSubject.subject_id;
        await apiClient.put(`/api/subjects/${id}`, {
          subject_name: formData.subjectName.trim(),
          subject_code: formData.subjectCode.trim(),
        });
        showSuccess("Subject updated successfully!");
      } else {
        await apiClient.post("/api/subjects", {
          subject_name: formData.subjectName.trim(),
          subject_code: formData.subjectCode.trim(),
        });
        showSuccess("Subject created successfully!");
      }
      setModalOpen(false);
      fetchSubjects();
    } catch (err) {
      showError(err.response?.data?.message || err.message || "Failed to save subject");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSubject = async (sub) => {
    const id = sub.subjectId || sub.subject_id;
    const name = sub.subjectName || sub.subject_name;
    if (!window.confirm(`Are you sure you want to delete subject "${name}"? Associated questions and data will be removed.`)) {
      return;
    }

    try {
      await apiClient.delete(`/api/subjects/${id}`);
      showSuccess(`Subject "${name}" deleted`);
      setSubjects((prev) => prev.filter((s) => (s.subjectId || s.subject_id) !== id));
    } catch (err) {
      showError(err.message || "Failed to delete subject");
    }
  };

  const filteredSubjects = subjects.filter((s) => {
    const name = (s.subjectName || s.subject_name || "").toLowerCase();
    const code = (s.subjectCode || s.subject_code || "").toLowerCase();
    const q = search.trim().toLowerCase();
    return !q || name.includes(q) || code.includes(q);
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Icon name="book" className="w-5 h-5 text-orange-500" />
            Subject & Curriculum Management
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Add, update, or remove subjects and audit curriculum question allocations
          </p>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all"
        >
          <Icon name="plus" className="w-4 h-4" />
          Add New Subject
        </button>
      </div>

      {/* Search & Stats Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by subject name or code..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>
        <div className="text-xs text-slate-500 font-medium px-2">
          Total Subjects: <span className="font-bold text-slate-800">{subjects.length}</span>
        </div>
      </div>

      {/* Subjects Grid */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading subjects...</p>
        </div>
      ) : filteredSubjects.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="book" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No subjects found</h3>
          <p className="text-xs text-slate-400">Click "Add New Subject" to create your first course discipline.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredSubjects.map((sub) => {
            const id = sub.subjectId || sub.subject_id;
            const name = sub.subjectName || sub.subject_name;
            const code = sub.subjectCode || sub.subject_code;
            const qCount = sub.questionCount || 0;
            const eCount = sub.examCount || 0;

            return (
              <div
                key={id}
                className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:border-orange-200 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[11px] font-mono font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded-md border border-orange-100">
                        {code}
                      </span>
                      <h3 className="text-base font-bold text-slate-900 mt-2">{name}</h3>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => handleOpenEditModal(sub)}
                        className="p-1.5 text-slate-400 hover:text-orange-600 hover:bg-orange-50 rounded-lg transition-colors"
                        title="Edit Subject"
                      >
                        <Icon name="edit" className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteSubject(sub)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                        title="Delete Subject"
                      >
                        <Icon name="trash" className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mt-4 pt-4 border-t border-slate-100 text-xs">
                    <div className="bg-slate-50 p-2.5 rounded-xl">
                      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Questions</span>
                      <span className="text-sm font-black text-slate-800">{qCount}</span>
                    </div>
                    <div className="bg-slate-50 p-2.5 rounded-xl">
                      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Exams</span>
                      <span className="text-sm font-black text-slate-800">{eCount}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-4 pt-3 flex items-center justify-between text-[11px] text-slate-400">
                  <span>ID: #{id}</span>
                  <span className="text-emerald-600 font-semibold flex items-center gap-1">
                    <Icon name="check-circle" className="w-3 h-3" /> Active
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add / Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-md overflow-hidden animate-scale-up">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50">
              <h3 className="font-bold text-slate-800 text-sm flex items-center gap-2">
                <Icon name="book" className="w-4 h-4 text-orange-500" />
                {editingSubject ? "Edit Subject" : "Create New Subject"}
              </h3>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-200 transition-colors"
              >
                <Icon name="x" className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveSubject} className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Subject Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Computer Networks & Security"
                  value={formData.subjectName}
                  onChange={(e) => setFormData({ ...formData, subjectName: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Subject Code (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. CS-401 (Auto-generated if empty)"
                  value={formData.subjectCode}
                  onChange={(e) => setFormData({ ...formData, subjectCode: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all flex items-center gap-1.5"
                >
                  {submitting ? "Saving..." : editingSubject ? "Update Subject" : "Create Subject"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
