import React, { useEffect, useState, useCallback } from "react";
import { PieChart, Pie, Tooltip, Legend, Cell, ResponsiveContainer } from "recharts";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function TotalUsers() {
  const [students, setStudents] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("STUDENTS"); // STUDENTS or TEACHERS
  const [search, setSearch] = useState("");

  const { showSuccess, showError } = useToast();

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const [tRes, sRes] = await Promise.all([
        apiClient.get("/api/admin/viewAllTeacher"),
        apiClient.get("/api/admin/viewAllStudent"),
      ]);
      setTeachers(Array.isArray(tRes.data) ? tRes.data : []);
      setStudents(Array.isArray(sRes.data) ? sRes.data : []);
    } catch (err) {
      showError(err.message || "Failed to load user rosters");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleDeleteStudent = async (studentId) => {
    if (!window.confirm("Are you sure you want to remove this student account?")) return;
    try {
      await apiClient.delete(`/api/admin/studentDeleteById/${studentId}`);
      showSuccess("Student account removed");
      setStudents((prev) => prev.filter((s) => s.student_id !== studentId && s.studentId !== studentId));
    } catch (err) {
      showError(err.message || "Failed to delete student");
    }
  };

  const handleDeleteTeacher = async (teacherId) => {
    if (!window.confirm("Are you sure you want to remove this instructor account?")) return;
    try {
      await apiClient.delete(`/api/admin/teacherDeleteById/${teacherId}`);
      showSuccess("Teacher account removed");
      setTeachers((prev) => prev.filter((t) => t.teacher_id !== teacherId && t.teacherId !== teacherId));
    } catch (err) {
      showError(err.message || "Failed to delete teacher");
    }
  };

  const chartData = [
    { name: "Students", value: students.length },
    { name: "Teachers", value: teachers.length },
  ];
  const COLORS = ["#ea580c", "#3b82f6"];

  const filteredList = (activeTab === "STUDENTS" ? students : teachers).filter((u) => {
    const name = (u.student_name || u.studentName || u.teacher_name || u.teacherName || u.name || "").toLowerCase();
    const email = (u.student_email || u.studentEmail || u.teacher_email || u.teacherEmail || u.email || "").toLowerCase();
    return name.includes(search.toLowerCase()) || email.includes(search.toLowerCase());
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="users" className="w-5 h-5 text-orange-500" />
          Active Platform Users Directory
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Manage enrolled students and registered faculty members
        </p>
      </div>

      {/* Summary Cards & Chart */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
            <Icon name="book" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Active Students</span>
            <p className="text-2xl font-black text-slate-900 mt-0.5">{students.length}</p>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <Icon name="users" className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Active Faculty</span>
            <p className="text-2xl font-black text-slate-900 mt-0.5">{teachers.length}</p>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center h-28">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius={25}
                outerRadius={45}
                paddingAngle={5}
                dataKey="value"
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend verticalAlign="middle" align="right" layout="vertical" iconSize={8} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Directory Filter & Search */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        {/* Tab switch */}
        <div className="flex bg-slate-100 p-1 rounded-xl w-full sm:w-auto">
          <button
            onClick={() => setActiveTab("STUDENTS")}
            className={`flex-1 sm:flex-initial px-4 py-2 text-xs font-bold rounded-lg transition-all ${
              activeTab === "STUDENTS" ? "bg-white text-orange-600 shadow-sm" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            Students ({students.length})
          </button>
          <button
            onClick={() => setActiveTab("TEACHERS")}
            className={`flex-1 sm:flex-initial px-4 py-2 text-xs font-bold rounded-lg transition-all ${
              activeTab === "TEACHERS" ? "bg-white text-orange-600 shadow-sm" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            Teachers ({teachers.length})
          </button>
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-72">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name or email..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>
      </div>

      {/* Users Table */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading user roster...</p>
        </div>
      ) : filteredList.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="users" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No users found</h3>
          <p className="text-xs text-slate-400">Try adjusting your search criteria</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">User ID</th>
                  <th className="py-3.5 px-4">Full Name</th>
                  <th className="py-3.5 px-4">Email Address</th>
                  <th className="py-3.5 px-4">Contact</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredList.map((user, idx) => {
                  const id = activeTab === "STUDENTS" ? (user.student_id || user.studentId) : (user.teacher_id || user.teacherId);
                  const name = user.student_name || user.studentName || user.teacher_name || user.teacherName || user.name;
                  const email = user.student_email || user.studentEmail || user.teacher_email || user.teacherEmail || user.email;
                  const contact = user.student_contact || user.studentContact || user.teacher_contact || user.teacherContact || user.contact || "N/A";

                  return (
                    <tr key={id || idx} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-400">#{id}</td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">{name}</td>
                      <td className="py-3.5 px-4 text-xs text-slate-600">{email}</td>
                      <td className="py-3.5 px-4 text-xs text-slate-400">{contact}</td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => activeTab === "STUDENTS" ? handleDeleteStudent(id) : handleDeleteTeacher(id)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                          title="Delete Account"
                        >
                          <Icon name="trash" className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
