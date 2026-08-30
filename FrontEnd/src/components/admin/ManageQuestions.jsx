import React, { useState, useEffect, useCallback } from "react";
import apiClient from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function ManageQuestions() {
  const [questions, setQuestions] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedSubject, setSelectedSubject] = useState("ALL");
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    subjectId: "",
    question: "",
    questionType: "MCQ",
    option1: "",
    option2: "",
    option3: "",
    option4: "",
    correctOption: "",
    sampleAnswer: "",
  });

  const { showSuccess, showError } = useToast();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [qRes, sRes] = await Promise.all([
        apiClient.get("/api/questions/all"),
        apiClient.get("/api/subjects/all"),
      ]);
      setQuestions(Array.isArray(qRes.data) ? qRes.data : []);
      setSubjects(Array.isArray(sRes.data) ? sRes.data : []);
    } catch (err) {
      showError(err.message || "Failed to load questions data");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleOpenAddModal = () => {
    setEditingQuestion(null);
    setFormData({
      subjectId: subjects.length > 0 ? subjects[0].subjectId || subjects[0].subject_id : "",
      question: "",
      questionType: "MCQ",
      option1: "",
      option2: "",
      option3: "",
      option4: "",
      correctOption: "",
      sampleAnswer: "",
    });
    setModalOpen(true);
  };

  const handleOpenEditModal = (q) => {
    setEditingQuestion(q);
    setFormData({
      subjectId: q.subjectId || q.subject_id || "",
      question: q.question || "",
      questionType: q.questionType || (q.option1 ? "MCQ" : "QUESTION_ANSWER"),
      option1: q.option1 || "",
      option2: q.option2 || "",
      option3: q.option3 || "",
      option4: q.option4 || "",
      correctOption: q.correctOption || q.correct_option || "",
      sampleAnswer: q.sampleAnswer || q.sample_answer || "",
    });
    setModalOpen(true);
  };

  const handleSaveQuestion = async (e) => {
    e.preventDefault();
    if (!formData.question.trim()) {
      showError("Please enter the question text");
      return;
    }
    if (!formData.subjectId) {
      showError("Please select a subject");
      return;
    }

    if (formData.questionType === "MCQ") {
      if (!formData.option1.trim() || !formData.option2.trim()) {
        showError("Please provide at least Option 1 and Option 2 for MCQ");
        return;
      }
      if (!formData.correctOption.trim()) {
        showError("Please specify the correct option value");
        return;
      }
    }

    setSubmitting(true);
    try {
      const payload = {
        subject_id: parseInt(formData.subjectId, 10),
        question: formData.question.trim(),
        question_type: formData.questionType,
        option1: formData.option1.trim(),
        option2: formData.option2.trim(),
        option3: formData.option3.trim(),
        option4: formData.option4.trim(),
        correct_option: formData.correctOption.trim(),
        sample_answer: formData.sampleAnswer.trim(),
      };

      if (editingQuestion) {
        const qId = editingQuestion.questionId || editingQuestion.question_id;
        await apiClient.put(`/api/questions/${qId}`, { ...payload, question_id: qId });
        showSuccess("Question updated successfully!");
      } else {
        await apiClient.post("/api/questions", [payload]);
        showSuccess("Question created successfully!");
      }

      setModalOpen(false);
      fetchData();
    } catch (err) {
      showError(err.response?.data?.message || err.message || "Failed to save question");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteQuestion = async (qId) => {
    if (!window.confirm("Are you sure you want to permanently delete this question?")) return;
    try {
      await apiClient.delete(`/api/questions/${qId}`);
      showSuccess("Question deleted successfully");
      setQuestions((prev) => prev.filter((q) => (q.questionId || q.question_id) !== qId));
    } catch (err) {
      showError(err.message || "Failed to delete question");
    }
  };

  const filteredQuestions = questions.filter((q) => {
    const qSubjectId = String(q.subjectId || q.subject_id);
    const matchesSubject = selectedSubject === "ALL" || qSubjectId === String(selectedSubject);
    const text = (q.question || "").toLowerCase();
    const query = search.trim().toLowerCase();
    const matchesSearch = !query || text.includes(query);
    return matchesSubject && matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Icon name="help-circle" className="w-5 h-5 text-orange-500" />
            Central Question Bank & Item Repository
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Audit, edit, and organize examination questions across all curriculum disciplines
          </p>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all"
        >
          <Icon name="plus" className="w-4 h-4" />
          Add New Question
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <label className="text-xs font-bold text-slate-600 whitespace-nowrap">Subject:</label>
          <select
            value={selectedSubject}
            onChange={(e) => setSelectedSubject(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 outline-none focus:border-orange-500 focus:bg-white"
          >
            <option value="ALL">All Subjects ({questions.length})</option>
            {subjects.map((s) => {
              const sId = s.subjectId || s.subject_id;
              const sName = s.subjectName || s.subject_name;
              return (
                <option key={sId} value={sId}>
                  {sName}
                </option>
              );
            })}
          </select>
        </div>

        <div className="relative w-full sm:w-80">
          <Icon name="search" className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search questions by text..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
          />
        </div>
      </div>

      {/* Questions List */}
      {loading ? (
        <div className="py-12 text-center">
          <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          <p className="text-xs text-slate-500">Loading question items...</p>
        </div>
      ) : filteredQuestions.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8 space-y-3">
          <Icon name="help-circle" className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-base font-bold text-slate-700">No questions found</h3>
          <p className="text-xs text-slate-400">Click "Add New Question" to insert test items into the repository.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredQuestions.map((q, idx) => {
            const qId = q.questionId || q.question_id;
            const isMcq = q.questionType === "MCQ" || Boolean(q.option1);
            const subName = q.subjectName || q.subject_name || (subjects.find((s) => (s.subjectId || s.subject_id) === (q.subjectId || q.subject_id))?.subjectName) || "General";

            return (
              <div
                key={qId || idx}
                className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-sm hover:border-orange-200 transition-all space-y-3"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-[11px] font-mono font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-md">
                        #{qId}
                      </span>
                      <span className="text-[11px] font-bold text-orange-700 bg-orange-50 px-2.5 py-0.5 rounded-full border border-orange-100">
                        {subName}
                      </span>
                      <span className="text-[11px] font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-full">
                        {isMcq ? "MCQ" : "Subjective"}
                      </span>
                    </div>
                    <h4 className="text-sm font-bold text-slate-900 leading-snug pt-1">
                      {q.question}
                    </h4>
                  </div>

                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => handleOpenEditModal(q)}
                      className="p-2 text-slate-400 hover:text-orange-600 hover:bg-orange-50 rounded-xl transition-colors"
                      title="Edit Question"
                    >
                      <Icon name="edit" className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteQuestion(qId)}
                      className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors"
                      title="Delete Question"
                    >
                      <Icon name="trash" className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Question Options or Rubric */}
                {isMcq ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 text-xs">
                    {[q.option1, q.option2, q.option3, q.option4].filter(Boolean).map((opt, i) => {
                      const isCorrect = q.correctOption && opt.trim().toLowerCase() === q.correctOption.trim().toLowerCase();
                      return (
                        <div
                          key={i}
                          className={`p-2.5 rounded-xl border flex items-center justify-between ${
                            isCorrect
                              ? "bg-emerald-50 border-emerald-200 text-emerald-900 font-semibold"
                              : "bg-slate-50 border-slate-200 text-slate-700"
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-white border text-[10px] font-black flex items-center justify-center text-slate-500">
                              {String.fromCharCode(65 + i)}
                            </span>
                            <span>{opt}</span>
                          </div>
                          {isCorrect && (
                            <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-md flex items-center gap-1">
                              <Icon name="check" className="w-3 h-3" /> Correct
                            </span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  q.sampleAnswer && (
                    <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-700 space-y-1">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Sample Answer / Evaluation Rubric:</span>
                      <p className="font-medium text-slate-800">{q.sampleAnswer}</p>
                    </div>
                  )
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Add / Edit Question Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-xl max-h-[90vh] overflow-y-auto animate-scale-up">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50 sticky top-0 z-10">
              <h3 className="font-bold text-slate-800 text-sm flex items-center gap-2">
                <Icon name="help-circle" className="w-4 h-4 text-orange-500" />
                {editingQuestion ? "Edit Question" : "Add Question to Bank"}
              </h3>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-200 transition-colors"
              >
                <Icon name="x" className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveQuestion} className="p-5 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Subject <span className="text-rose-500">*</span>
                  </label>
                  <select
                    required
                    value={formData.subjectId}
                    onChange={(e) => setFormData({ ...formData, subjectId: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 outline-none focus:border-orange-500 focus:bg-white"
                  >
                    <option value="">Select a Subject</option>
                    {subjects.map((s) => (
                      <option key={s.subjectId || s.subject_id} value={s.subjectId || s.subject_id}>
                        {s.subjectName || s.subject_name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Question Type</label>
                  <select
                    value={formData.questionType}
                    onChange={(e) => setFormData({ ...formData, questionType: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 outline-none focus:border-orange-500 focus:bg-white"
                  >
                    <option value="MCQ">Multiple Choice (MCQ)</option>
                    <option value="QUESTION_ANSWER">Subjective / QA</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Question Text <span className="text-rose-500">*</span>
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Enter the full question prompt here..."
                  value={formData.question}
                  onChange={(e) => setFormData({ ...formData, question: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              {formData.questionType === "MCQ" ? (
                <div className="space-y-3 pt-2 border-t border-slate-100">
                  <span className="text-xs font-bold text-slate-700 block">MCQ Options</span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-500 mb-1">Option A</label>
                      <input
                        type="text"
                        placeholder="Option A content"
                        value={formData.option1}
                        onChange={(e) => setFormData({ ...formData, option1: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-500 mb-1">Option B</label>
                      <input
                        type="text"
                        placeholder="Option B content"
                        value={formData.option2}
                        onChange={(e) => setFormData({ ...formData, option2: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-500 mb-1">Option C</label>
                      <input
                        type="text"
                        placeholder="Option C content"
                        value={formData.option3}
                        onChange={(e) => setFormData({ ...formData, option3: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-500 mb-1">Option D</label>
                      <input
                        type="text"
                        placeholder="Option D content"
                        value={formData.option4}
                        onChange={(e) => setFormData({ ...formData, option4: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Correct Answer Option <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="Exact text matching the correct option (e.g. Option A's text)"
                      value={formData.correctOption}
                      onChange={(e) => setFormData({ ...formData, correctOption: e.target.value })}
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                    />
                  </div>
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Sample Answer / Evaluation Rubric
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Provide a benchmark answer or scoring criteria..."
                    value={formData.sampleAnswer}
                    onChange={(e) => setFormData({ ...formData, sampleAnswer: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500 focus:bg-white"
                  />
                </div>
              )}

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
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
                  {submitting ? "Saving..." : editingQuestion ? "Update Question" : "Save Question"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
