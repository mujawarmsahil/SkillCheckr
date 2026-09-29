import React, { useState, useEffect, useCallback } from "react";
import { getAllResults } from "../../api/resultApi";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function ManageResults() {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedResult, setSelectedResult] = useState(null);

  const { showError } = useToast();

  const fetchResults = useCallback(async () => {
    setLoading(true);
    try {
      setResults(await getAllResults());
    } catch (err) {
      showError(err.message || "Failed to load results");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchResults();
  }, [fetchResults]);

  const totalSubmissions = results.length;
  const passedCount = results.filter((r) => (r.status || "").toLowerCase().includes("pass")).length;
  const failedCount = results.filter((r) => (r.status || "").toLowerCase().includes("fail")).length;
  const disqualifiedCount = results.filter((r) => (r.status || "").toLowerCase().includes("disqualified")).length;

  const filteredResults = results.filter((r) => {
    const sName = (r.student_name || r.studentName || "").toLowerCase();
    const eName = (r.exam_name || r.examName || "").toLowerCase();
    const subName = (r.subject_name || r.subjectName || "").toLowerCase();
    const query = search.trim().toLowerCase();
    const matchesSearch = !query || sName.includes(query) || eName.includes(query) || subName.includes(query);

    const st = (r.status || "").toLowerCase();
    let matchesStatus = true;
    if (statusFilter === "PASS") matchesStatus = st.includes("pass");
    else if (statusFilter === "FAIL") matchesStatus = st.includes("fail");
    else if (statusFilter === "DISQUALIFIED") matchesStatus = st.includes("disqualified");

    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
          <Icon name="award" className="w-5 h-5 text-orange-500" />
          Results
        </h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Review student submissions, scores, and proctoring flags
        </p>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Attempts</span>
          <p className="text-2xl font-black text-slate-900 mt-1">{totalSubmissions}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider block">Passed</span>
          <p className="text-2xl font-black text-emerald-600 mt-1">{passedCount}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[10px] font-bold text-rose-600 uppercase tracking-wider block">Failed</span>
          <p className="text-2xl font-black text-rose-600 mt-1">{failedCount}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[10px] font-bold text-amber-600 uppercase tracking-wider block">Disqualified</span>
          <p className="text-2xl font-black text-amber-600 mt-1">{disqualifiedCount}</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by student, exam, or subject"
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>

        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {[
            { id: "ALL", label: "All Results" },
            { id: "PASS", label: "Passed" },
            { id: "FAIL", label: "Failed" },
            { id: "DISQUALIFIED", label: "Disqualified" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                statusFilter === tab.id
                  ? "bg-slate-900 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading student results...</p>
        </div>
      ) : filteredResults.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="award" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No results recorded</h3>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">Result ID</th>
                  <th className="py-3.5 px-4">Student</th>
                  <th className="py-3.5 px-4">Exam Title</th>
                  <th className="py-3.5 px-4">Subject</th>
                  <th className="py-3.5 px-4">Score</th>
                  <th className="py-3.5 px-4">Percentage</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredResults.map((res, idx) => {
                  const rId = res.result_id || res.resultId;
                  const sName = res.student_name || res.studentName || `Student #${res.student_id || res.studentId}`;
                  const eName = res.exam_name || res.examName || `Exam #${res.exam_id || res.examId}`;
                  const subName = res.subject_name || res.subjectName || "General";
                  const marks = res.marks_obtained !== undefined ? res.marks_obtained : res.marksObtained;
                  const total = res.total_marks || res.totalMarks || 100;
                  const pct = res.percentage !== undefined ? Math.round(res.percentage) : 0;
                  const st = res.status || "Submitted";
                  const isPass = st.toLowerCase().includes("pass");
                  const isFail = st.toLowerCase().includes("fail");
                  const isDisqualified = st.toLowerCase().includes("disqualified");

                  return (
                    <tr key={rId || idx} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-400">#{rId}</td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-slate-900">{sName}</div>
                        <div className="text-[11px] text-slate-400">ID: #{res.student_id || res.studentId}</div>
                      </td>
                      <td className="py-3.5 px-4 font-medium text-slate-800">{eName}</td>
                      <td className="py-3.5 px-4">
                        <span className="text-xs font-medium bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                          {subName}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-bold text-slate-900">
                        {marks} / {total}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-700">{pct}%</td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`text-xs font-bold px-2.5 py-0.5 rounded-full inline-flex items-center gap-1 ${
                            isPass
                              ? "bg-emerald-100 text-emerald-800"
                              : isFail
                              ? "bg-rose-100 text-rose-800"
                              : isDisqualified
                              ? "bg-amber-100 text-amber-800"
                              : "bg-blue-100 text-blue-800"
                          }`}
                        >
                          {isPass && <Icon name="check-circle" className="w-3 h-3 text-emerald-600" />}
                          {isFail && <Icon name="x" className="w-3 h-3 text-rose-600" />}
                          {isDisqualified && <Icon name="alert" className="w-3 h-3 text-amber-600" />}
                          {st}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => setSelectedResult(res)}
                          className="px-2.5 py-1 text-xs font-semibold text-orange-600 hover:text-orange-700 hover:bg-orange-50 rounded-lg transition-colors inline-flex items-center gap-1"
                        >
                          <Icon name="eye" className="w-3.5 h-3.5" />
                          View
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

      {selectedResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg max-h-[85vh] overflow-y-auto animate-scale-up">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50 sticky top-0 z-10">
              <h3 className="font-bold text-slate-800 text-sm flex items-center gap-2">
                <Icon name="award" className="w-4 h-4 text-orange-500" />
                Result #{selectedResult.result_id || selectedResult.resultId}
              </h3>
              <button
                onClick={() => setSelectedResult(null)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-200 transition-colors"
              >
                <Icon name="x" className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <span className="text-[10px] text-slate-400 font-bold uppercase block">Student</span>
                  <p className="font-bold text-slate-900 mt-0.5">
                    {selectedResult.student_name || selectedResult.studentName || `Student #${selectedResult.student_id || selectedResult.studentId}`}
                  </p>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <span className="text-[10px] text-slate-400 font-bold uppercase block">Exam</span>
                  <p className="font-bold text-slate-900 mt-0.5">
                    {selectedResult.exam_name || selectedResult.examName}
                  </p>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <span className="text-[10px] text-slate-400 font-bold uppercase block">Subject</span>
                  <p className="font-bold text-slate-900 mt-0.5">
                    {selectedResult.subject_name || selectedResult.subjectName || "General"}
                  </p>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <span className="text-[10px] text-slate-400 font-bold uppercase block">Submitted At</span>
                  <p className="font-bold text-slate-900 mt-0.5">
                    {selectedResult.submitted_at || selectedResult.submittedAt || "N/A"}
                  </p>
                </div>
              </div>

              <div className="p-4 bg-orange-50/50 rounded-2xl border border-orange-100 flex items-center justify-between">
                <div>
                  <span className="text-xs text-orange-700 font-semibold block">Total Score</span>
                  <div className="text-2xl font-black text-orange-950 mt-0.5">
                    {selectedResult.marks_obtained !== undefined ? selectedResult.marks_obtained : selectedResult.marksObtained} / {selectedResult.total_marks || selectedResult.totalMarks || 100}
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-xs text-orange-700 font-semibold block">Percentage</span>
                  <div className="text-2xl font-black text-orange-950 mt-0.5">
                    {selectedResult.percentage !== undefined ? Math.round(selectedResult.percentage) : 0}%
                  </div>
                </div>
              </div>

              {selectedResult.question_breakdown && selectedResult.question_breakdown.length > 0 && (
                <div className="space-y-2 pt-2">
                  <span className="text-xs font-bold text-slate-700 block">Performance by question</span>
                  <div className="space-y-2 max-h-60 overflow-y-auto">
                    {selectedResult.question_breakdown.map((q, i) => (
                      <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-800">Q{i + 1}: {q.question}</span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${q.isCorrect ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-800"}`}>
                            {q.isCorrect ? "Correct" : "Incorrect"}
                          </span>
                        </div>
                        <div className="text-slate-600">Answer: <span className="font-semibold">{q.selectedAnswer || "None"}</span></div>
                        <div className="text-slate-600">Correct Option: <span className="font-semibold text-emerald-700">{q.correctAnswer}</span></div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex justify-end pt-3 border-t border-slate-100">
                <button
                  onClick={() => setSelectedResult(null)}
                  className="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-xl hover:bg-slate-800 transition-all"
                >
                  Close Result
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
