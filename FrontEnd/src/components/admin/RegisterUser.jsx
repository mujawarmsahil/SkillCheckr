import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function RegisterUser() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterRole, setFilterRole] = useState("ALL");
  const { showSuccess, showError, showWarning } = useToast();

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/requests/viewAllRegisterUsers");
      setRequests(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      showError(err.message || "Failed to load registration requests");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleApprove = async (request, roleToApprove) => {
    if (request.status === "Approved") {
      showWarning("This user request is already approved.");
      return;
    }

    const requestId = request.request_id || request.requestId;
    const targetRole = roleToApprove || request.requested_role || request.requestedRole;

    try {
      const endpoint = targetRole === "Teacher" 
        ? `/api/admin/addTeacher/${requestId}`
        : `/api/admin/addStudent/${requestId}`;

      await apiClient.post(endpoint);
      showSuccess(`Approved ${request.name} as a ${targetRole}!`);
      
      setRequests((prev) =>
        prev.map((r) =>
          (r.request_id === requestId || r.requestId === requestId)
            ? { ...r, status: "Approved" }
            : r
        )
      );
    } catch (err) {
      showError(err.message || "Failed to approve registration request");
    }
  };

  const handleDelete = async (requestId) => {
    if (!window.confirm("Are you sure you want to remove this registration request?")) {
      return;
    }

    try {
      await apiClient.delete(`/api/requests/deleteById/${requestId}`);
      showSuccess("Registration request removed");
      setRequests((prev) => prev.filter((r) => r.request_id !== requestId && r.requestId !== requestId));
    } catch (err) {
      showError(err.message || "Failed to delete request");
    }
  };

  const filteredRequests = requests.filter((r) => {
    const name = (r.name || "").toLowerCase();
    const email = (r.email || "").toLowerCase();
    const username = (r.username || "").toLowerCase();
    const matchesSearch = name.includes(search.toLowerCase()) || email.includes(search.toLowerCase()) || username.includes(search.toLowerCase());

    const reqRole = (r.requested_role || r.requestedRole || "").toUpperCase();
    const matchesRole = filterRole === "ALL" || reqRole === filterRole;

    return matchesSearch && matchesRole;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="user-plus" className="w-5 h-5 text-orange-500" />
          Pending Account Registration Requests
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Review and approve incoming student and teacher access requests
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative flex-1 w-full">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, email, or username..."
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto">
          {[
            { id: "ALL", label: "All Requests" },
            { id: "STUDENT", label: "Students" },
            { id: "TEACHER", label: "Teachers" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterRole(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                filterRole === tab.id
                  ? "bg-slate-900 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading requests...</p>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="check-circle" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No pending registration requests</h3>
          <p className="text-xs text-slate-400">All student and instructor registrations have been reviewed.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">Applicant</th>
                  <th className="py-3.5 px-4">Contact Info</th>
                  <th className="py-3.5 px-4">Requested Role</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Approval Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredRequests.map((req) => {
                  const reqId = req.request_id || req.requestId;
                  const isApproved = req.status === "Approved";
                  const role = req.requested_role || req.requestedRole || "Student";

                  return (
                    <tr key={reqId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-slate-900">{req.name}</div>
                        <div className="text-xs text-slate-400">@{req.username}</div>
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-600">
                        <div>{req.email}</div>
                        <div className="text-slate-400">{req.contact || "N/A"}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-1 rounded-md ${
                            role === "Teacher" ? "bg-purple-100 text-purple-800" : "bg-blue-100 text-blue-800"
                          }`}
                        >
                          {role}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                            isApproved ? "bg-emerald-100 text-emerald-800" : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {req.status || "Pending"}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {!isApproved ? (
                            <>
                              <button
                                onClick={() => handleApprove(req, role)}
                                className="py-1.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-all flex items-center gap-1"
                              >
                                <Icon name="check" className="w-3.5 h-3.5" />
                                Approve
                              </button>
                            </>
                          ) : (
                            <span className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
                              <Icon name="check-circle" className="w-4 h-4" />
                              Active
                            </span>
                          )}

                          <button
                            onClick={() => handleDelete(reqId)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                            title="Reject/Delete Request"
                          >
                            <Icon name="trash" className="w-4 h-4" />
                          </button>
                        </div>
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
