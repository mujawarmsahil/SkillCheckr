import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function RegisterUser() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentTab, setCurrentTab] = useState("PENDING"); // "PENDING" or "ARCHIVE"
  const [search, setSearch] = useState("");
  const [filterRole, setFilterRole] = useState("ALL");
  const [filterArchiveStatus, setFilterArchiveStatus] = useState("ALL"); // "ALL", "APPROVED", "REJECTED"
  const { showSuccess, showError, showWarning } = useToast();

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/requests/viewAllRegisterUsers");
      setRequests(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      if (err.status === 404) {
        setRequests([]);
      } else {
        showError(err.message || "Failed to load registration requests");
      }
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
    const targetRole = roleToApprove || request.requested_role || request.requestedRole || "Student";

    try {
      const endpoint = targetRole === "Teacher"
        ? `/api/admin/addTeacher/${requestId}`
        : `/api/admin/addStudent/${requestId}`;

      await apiClient.post(endpoint);
      showSuccess(`Approved ${request.name || request.username} as a ${targetRole}! Moved to archive.`);

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

  const handleReject = async (request) => {
    const requestId = request.request_id || request.requestId;
    try {
      await apiClient.post(`/api/requests/reject/${requestId}`);
      showSuccess(`Registration request for ${request.name || request.username} was rejected and moved to archive.`);

      setRequests((prev) =>
        prev.map((r) =>
          (r.request_id === requestId || r.requestId === requestId)
            ? { ...r, status: "Rejected" }
            : r
        )
      );
    } catch (err) {
      showError(err.message || "Failed to reject registration request");
    }
  };

  const handleDeletePermanently = async (requestId) => {
    if (!window.confirm("Are you sure you want to permanently delete this archived request?")) {
      return;
    }

    try {
      await apiClient.delete(`/api/requests/deleteById/${requestId}`);
      showSuccess("Archived request permanently removed.");
      setRequests((prev) => prev.filter((r) => r.request_id !== requestId && r.requestId !== requestId));
    } catch (err) {
      showError(err.message || "Failed to delete request");
    }
  };

  // Split requests into Pending and Archived lists
  const pendingRequestsList = requests.filter((r) => {
    const status = (r.status || "").trim().toLowerCase();
    return !status || status === "pending";
  });

  const archivedRequestsList = requests.filter((r) => {
    const status = (r.status || "").trim().toLowerCase();
    return status && status !== "pending";
  });

  // Filter based on active tab, search term, and role/status filters
  const activeList = currentTab === "PENDING" ? pendingRequestsList : archivedRequestsList;

  const filteredRequests = activeList.filter((r) => {
    const name = (r.name || "").toLowerCase();
    const email = (r.email || "").toLowerCase();
    const username = (r.username || "").toLowerCase();
    const contact = (r.contact || "").toLowerCase();
    const query = search.trim().toLowerCase();
    const matchesSearch = !query || name.includes(query) || email.includes(query) || username.includes(query) || contact.includes(query);

    const reqRole = (r.requested_role || r.requestedRole || "").toUpperCase();
    const matchesRole = filterRole === "ALL" || reqRole === filterRole;

    if (currentTab === "ARCHIVE") {
      const status = (r.status || "").toUpperCase();
      const matchesArchiveStatus =
        filterArchiveStatus === "ALL" ||
        (filterArchiveStatus === "APPROVED" && status === "APPROVED") ||
        (filterArchiveStatus === "REJECTED" && (status === "REJECTED" || status === "DELETED"));
      return matchesSearch && matchesRole && matchesArchiveStatus;
    }

    return matchesSearch && matchesRole;
  });

  return (
    <div className="space-y-6">
      {/* Header and Tab Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Icon name="user-plus" className="w-5 h-5 text-orange-500" />
            Account Registration Requests
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage incoming student and teacher access requests and inspect approval history
          </p>
        </div>

        {/* Pending vs Archive Tab Pill Switcher */}
        <div className="flex items-center p-1 bg-slate-100 rounded-2xl border border-slate-200 w-full sm:w-auto">
          <button
            onClick={() => {
              setCurrentTab("PENDING");
              setSearch("");
            }}
            className={`flex-1 sm:flex-initial flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              currentTab === "PENDING"
                ? "bg-white text-slate-900 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <Icon name="clock" className="w-4 h-4 text-amber-500" />
            <span>Pending Requests</span>
            <span
              className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                currentTab === "PENDING" ? "bg-amber-100 text-amber-800" : "bg-slate-200 text-slate-600"
              }`}
            >
              {pendingRequestsList.length}
            </span>
          </button>

          <button
            onClick={() => {
              setCurrentTab("ARCHIVE");
              setSearch("");
            }}
            className={`flex-1 sm:flex-initial flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              currentTab === "ARCHIVE"
                ? "bg-white text-slate-900 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <Icon name="archive" className="w-4 h-4 text-emerald-600" />
            <span>Archive</span>
            <span
              className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                currentTab === "ARCHIVE" ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-600"
              }`}
            >
              {archivedRequestsList.length}
            </span>
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative flex-1 w-full">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={
              currentTab === "PENDING"
                ? "Search pending requests by name, email, or username..."
                : "Search archived requests by name, email, or username..."
            }
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        {/* Role Filters */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto">
          {[
            { id: "ALL", label: "All Roles" },
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

        {/* Status Filter for Archive Tab */}
        {currentTab === "ARCHIVE" && (
          <div className="flex items-center gap-1.5 w-full sm:w-auto border-t sm:border-t-0 sm:border-l border-slate-200 pt-2 sm:pt-0 sm:pl-3">
            {[
              { id: "ALL", label: "All Status" },
              { id: "APPROVED", label: "Approved" },
              { id: "REJECTED", label: "Rejected" },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setFilterArchiveStatus(tab.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                  filterArchiveStatus === tab.id
                    ? "bg-orange-500 text-white shadow-sm"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Table Content */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading requests...</p>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3 shadow-sm">
          {currentTab === "PENDING" ? (
            <>
              <Icon name="check-circle" className="w-10 h-10 text-emerald-500 mx-auto" />
              <h3 className="text-base font-bold text-slate-700">No pending registration requests</h3>
              <p className="text-xs text-slate-400">
                All student and teacher registration requests have been reviewed and moved to the archive.
              </p>
            </>
          ) : (
            <>
              <Icon name="archive" className="w-10 h-10 text-slate-300 mx-auto" />
              <h3 className="text-base font-bold text-slate-700">No archived requests found</h3>
              <p className="text-xs text-slate-400">
                Approved and rejected requests will be archived here for historical tracking.
              </p>
            </>
          )}
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
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredRequests.map((req) => {
                  const reqId = req.request_id || req.requestId;
                  const statusStr = (req.status || "Pending").trim();
                  const isApproved = statusStr.toLowerCase() === "approved";
                  const isRejected = statusStr.toLowerCase() === "rejected";
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
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full inline-flex items-center gap-1 ${
                            isApproved
                              ? "bg-emerald-100 text-emerald-800"
                              : isRejected
                              ? "bg-rose-100 text-rose-800"
                              : "bg-amber-100 text-amber-800"
                          }`}
                        >
                          {isApproved && <Icon name="check-circle" className="w-3 h-3 text-emerald-600" />}
                          {isRejected && <Icon name="x" className="w-3 h-3 text-rose-600" />}
                          {!isApproved && !isRejected && <Icon name="clock" className="w-3 h-3 text-amber-600" />}
                          {statusStr}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {currentTab === "PENDING" ? (
                            <>
                              <button
                                onClick={() => handleApprove(req, role)}
                                className="py-1.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-all flex items-center gap-1"
                                title="Approve and create user account"
                              >
                                <Icon name="check" className="w-3.5 h-3.5" />
                                Approve
                              </button>
                              <button
                                onClick={() => handleReject(req)}
                                className="py-1.5 px-2.5 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-semibold rounded-lg border border-rose-200 transition-all flex items-center gap-1"
                                title="Reject and move to archive"
                              >
                                <Icon name="x" className="w-3.5 h-3.5" />
                                Reject
                              </button>
                            </>
                          ) : (
                            <>
                              {isApproved ? (
                                <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-md border border-emerald-200 flex items-center gap-1">
                                  <Icon name="check-circle" className="w-3.5 h-3.5 text-emerald-600" />
                                  Account Active
                                </span>
                              ) : (
                                <button
                                  onClick={() => handleApprove(req, role)}
                                  className="py-1.5 px-2.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-semibold rounded-lg border border-emerald-200 transition-all flex items-center gap-1"
                                  title="Re-approve this request"
                                >
                                  <Icon name="refresh" className="w-3.5 h-3.5" />
                                  Re-approve
                                </button>
                              )}

                              <button
                                onClick={() => handleDeletePermanently(reqId)}
                                className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                                title="Permanently delete from database"
                              >
                                <Icon name="trash" className="w-4 h-4" />
                              </button>
                            </>
                          )}
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
