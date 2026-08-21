import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function AddExam({ onExamCreated }) {
  const { user } = useAuth();
  const { showSuccess, showError, showWarning } = useToast();
  const navigate = useNavigate();

  // Step 1: Exam Info, Step 2: Questions Builder
  const [step, setStep] = useState(1);

  const [examData, setExamData] = useState({
    examName: "",
    subjectName: "",
    subjectCode: "",
    examType: "MCQ", // "MCQ" or "QUESTION_ANSWER"
    startDate: new Date().toISOString().split("T")[0],
    startTime: "10:00",
    endTime: "11:00",
    durationMinutes: 60,
    totalMarks: 50,
    passingMarks: 20,
  });

  const [currentQuestion, setCurrentQuestion] = useState({
    question: "",
    option1: "",
    option2: "",
    option3: "",
    option4: "",
    correctOption: "",
    sampleAnswer: "",
    marks: 1,
    wordLimit: 200,
  });

  const [questionsList, setQuestionsList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [savedSubjectId, setSavedSubjectId] = useState(null);

  // Auto calculate duration when start or end time changes
  useEffect(() => {
    if (examData.startTime && examData.endTime) {
      const [sh, sm] = examData.startTime.split(":").map(Number);
      const [eh, em] = examData.endTime.split(":").map(Number);
      const totalStartMinutes = sh * 60 + sm;
      const totalEndMinutes = eh * 60 + em;
      const diff = totalEndMinutes - totalStartMinutes;
      if (diff > 0) {
        setExamData((prev) => ({ ...prev, durationMinutes: diff }));
      }
    }
  }, [examData.startTime, examData.endTime]);

  const handleExamChange = (e) => {
    const { name, value } = e.target;
    let val = value;
    if (["totalMarks", "passingMarks", "durationMinutes"].includes(name)) {
      val = parseInt(value, 10) || 0;
    }
    setExamData((prev) => ({ ...prev, [name]: val }));
  };

  const handleQuestionChange = (e) => {
    const { name, value } = e.target;
    let val = value;
    if (["marks", "wordLimit"].includes(name)) {
      val = parseInt(value, 10) || 0;
    }
    setCurrentQuestion((prev) => ({ ...prev, [name]: val }));
  };

  const validateStep1 = () => {
    if (!examData.examName.trim()) {
      showWarning("Please enter an Exam Name");
      return false;
    }
    if (!examData.subjectName.trim() || !examData.subjectCode.trim()) {
      showWarning("Please provide Subject Name and Code");
      return false;
    }
    if (!examData.startDate) {
      showWarning("Please select a valid Exam Date");
      return false;
    }
    if (examData.durationMinutes <= 0) {
      showWarning("End Time must be after Start Time");
      return false;
    }
    if (examData.totalMarks <= 0) {
      showWarning("Total marks must be greater than 0");
      return false;
    }
    if (examData.passingMarks <= 0 || examData.passingMarks > examData.totalMarks) {
      showWarning("Passing marks must be between 1 and Total Marks");
      return false;
    }
    return true;
  };

  const handleProceedToQuestions = async (e) => {
    e.preventDefault();
    if (!validateStep1()) return;

    setLoading(true);
    try {
      const teacherId = user?.roleId || localStorage.getItem("teacher_id") || 1;
      const dateTime = `${examData.startDate}T${examData.startTime}`;

      const payload = {
        exam_name: examData.examName,
        exam_type: examData.examType,
        subject: {
          subject_name: examData.subjectName,
          subject_code: examData.subjectCode,
          subjectName: examData.subjectName,
          subjectCode: examData.subjectCode,
        },
        teacher_id: parseInt(teacherId, 10),
        date: dateTime,
        start_time: examData.startTime,
        end_time: examData.endTime,
        duration_minutes: examData.durationMinutes,
        total_marks: examData.totalMarks,
        passing_marks: examData.passingMarks,
        status: "Upcoming",
      };

      const response = await apiClient.post("/api/exams/addExams", payload);
      const subjectId = response.data?.subjectId || response.data?.subject_id || response.data?.id;

      if (subjectId) {
        setSavedSubjectId(subjectId);
        showSuccess(`Exam created! Now add ${examData.examType === "MCQ" ? "MCQ" : "Question-Answer"} questions.`);
        setStep(2);
      } else {
        showError("Could not retrieve subject ID for question mapping");
      }
    } catch (err) {
      showError(err.message || "Failed to create exam metadata");
    } finally {
      setLoading(false);
    }
  };

  const handleAddQuestion = () => {
    if (!currentQuestion.question.trim()) {
      showWarning("Question prompt cannot be empty");
      return;
    }

    if (examData.examType === "MCQ") {
      if (
        !currentQuestion.option1.trim() ||
        !currentQuestion.option2.trim() ||
        !currentQuestion.option3.trim() ||
        !currentQuestion.option4.trim()
      ) {
        showWarning("Please fill in all 4 options for MCQ");
        return;
      }
      if (!currentQuestion.correctOption) {
        showWarning("Please select the correct option");
        return;
      }
    }

    setQuestionsList((prev) => [
      ...prev,
      {
        ...currentQuestion,
        questionType: examData.examType,
        id: Date.now(),
      },
    ]);

    // Reset current question input
    setCurrentQuestion({
      question: "",
      option1: "",
      option2: "",
      option3: "",
      option4: "",
      correctOption: "",
      sampleAnswer: "",
      marks: 1,
      wordLimit: 200,
    });
    showSuccess("Question added to list!");
  };

  const handleRemoveQuestion = (index) => {
    setQuestionsList((prev) => prev.filter((_, i) => i !== index));
  };

  const handleFinalSubmit = async () => {
    if (questionsList.length === 0) {
      showWarning("Please add at least one question before publishing");
      return;
    }

    setLoading(true);
    try {
      const payload = questionsList.map((q) => ({
        subject_id: savedSubjectId,
        subjectId: savedSubjectId,
        question: q.question,
        question_type: examData.examType,
        questionType: examData.examType,
        option1: q.option1,
        option2: q.option2,
        option3: q.option3,
        option4: q.option4,
        correct_option: q.correctOption,
        correctOption: q.correctOption,
        sample_answer: q.sampleAnswer,
        sampleAnswer: q.sampleAnswer,
        marks: q.marks || 1,
        word_limit: q.wordLimit || 200,
        wordLimit: q.wordLimit || 200,
      }));

      await apiClient.post("/api/create/addQues", payload);
      showSuccess(`Exam & ${questionsList.length} questions published successfully!`);

      if (onExamCreated) {
        onExamCreated();
      } else {
        navigate("/dashboard/teacher");
      }
    } catch (err) {
      showError(err.message || "Failed to submit questions");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* Header & Steps Indicator */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Icon name="plus" className="w-5 h-5 text-orange-500" />
            Create New Examination
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Configure exam format, grading parameters, and question set
          </p>
        </div>

        {/* Step Indicator Pills */}
        <div className="flex items-center gap-2 bg-slate-100 p-1 rounded-xl text-xs font-semibold">
          <span
            className={`px-3 py-1.5 rounded-lg transition-all ${
              step === 1 ? "bg-orange-500 text-white shadow-sm" : "text-slate-600"
            }`}
          >
            1. Exam Details
          </span>
          <span
            className={`px-3 py-1.5 rounded-lg transition-all ${
              step === 2 ? "bg-orange-500 text-white shadow-sm" : "text-slate-400"
            }`}
          >
            2. Question Set ({questionsList.length})
          </span>
        </div>
      </div>

      {step === 1 && (
        <form onSubmit={handleProceedToQuestions} className="space-y-6">
          {/* Exam Type Selection */}
          <div className="bg-orange-50/60 border border-orange-200/80 rounded-2xl p-5">
            <label className="block text-xs font-bold text-orange-950 uppercase tracking-wider mb-2">
              Select Exam Format
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* MCQ Option */}
              <div
                onClick={() => setExamData((prev) => ({ ...prev, examType: "MCQ" }))}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  examData.examType === "MCQ"
                    ? "bg-white border-orange-500 shadow-md ring-2 ring-orange-200"
                    : "bg-white/60 border-slate-200 hover:border-slate-300"
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-sm">
                      MCQ
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-slate-800">Multiple Choice Questions</h4>
                      <p className="text-xs text-slate-500">4 Options with single correct answer. Auto-graded.</p>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="examType"
                    checked={examData.examType === "MCQ"}
                    onChange={() => {}}
                    className="h-4 w-4 text-orange-500 focus:ring-orange-400"
                  />
                </div>
              </div>

              {/* Question-Answer / Descriptive Option */}
              <div
                onClick={() => setExamData((prev) => ({ ...prev, examType: "QUESTION_ANSWER" }))}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  examData.examType === "QUESTION_ANSWER"
                    ? "bg-white border-orange-500 shadow-md ring-2 ring-orange-200"
                    : "bg-white/60 border-slate-200 hover:border-slate-300"
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm">
                      Q&A
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-slate-800">Question & Answer (Descriptive)</h4>
                      <p className="text-xs text-slate-500">Theory, short/long written responses & sample rubric.</p>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="examType"
                    checked={examData.examType === "QUESTION_ANSWER"}
                    onChange={() => {}}
                    className="h-4 w-4 text-orange-500 focus:ring-orange-400"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Primary Metadata */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-3">
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Exam Title
              </label>
              <input
                type="text"
                name="examName"
                value={examData.examName}
                onChange={handleExamChange}
                placeholder="e.g. Data Structures & Algorithms Midterm"
                className="w-full px-4 py-2.5 bg-white border border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200 rounded-xl text-slate-800 text-sm outline-none transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Subject Name
              </label>
              <input
                type="text"
                name="subjectName"
                value={examData.subjectName}
                onChange={handleExamChange}
                placeholder="e.g. Computer Science"
                className="w-full px-4 py-2.5 bg-white border border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200 rounded-xl text-slate-800 text-sm outline-none transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Subject Code
              </label>
              <input
                type="text"
                name="subjectCode"
                value={examData.subjectCode}
                onChange={handleExamChange}
                placeholder="e.g. CS-301"
                className="w-full px-4 py-2.5 bg-white border border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200 rounded-xl text-slate-800 text-sm outline-none transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Exam Date
              </label>
              <input
                type="date"
                name="startDate"
                value={examData.startDate}
                onChange={handleExamChange}
                className="w-full px-4 py-2.5 bg-white border border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200 rounded-xl text-slate-800 text-sm outline-none transition-all"
                required
              />
            </div>
          </div>

          {/* Time & Duration */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 bg-slate-50 p-4 rounded-2xl border border-slate-200">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Start Time
              </label>
              <input
                type="time"
                name="startTime"
                value={examData.startTime}
                onChange={handleExamChange}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                End Time
              </label>
              <input
                type="time"
                name="endTime"
                value={examData.endTime}
                onChange={handleExamChange}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Duration (Minutes)
              </label>
              <input
                type="number"
                name="durationMinutes"
                value={examData.durationMinutes}
                onChange={handleExamChange}
                min="5"
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm outline-none"
                required
              />
            </div>
          </div>

          {/* Marks */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Total Marks
              </label>
              <input
                type="number"
                name="totalMarks"
                value={examData.totalMarks}
                onChange={handleExamChange}
                min="1"
                className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Passing Marks
              </label>
              <input
                type="number"
                name="passingMarks"
                value={examData.passingMarks}
                onChange={handleExamChange}
                min="1"
                max={examData.totalMarks}
                className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm outline-none"
                required
              />
            </div>
          </div>

          {/* Action Button */}
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={loading}
              className="py-3 px-6 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all flex items-center gap-2"
            >
              {loading ? "Creating..." : "Proceed to Add Questions →"}
            </button>
          </div>
        </form>
      )}

      {step === 2 && (
        <div className="space-y-6">
          {/* Question Builder Box */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider px-2.5 py-1 rounded-md bg-orange-100 text-orange-700">
                {examData.examType === "MCQ" ? "New Multiple Choice Question" : "New Question & Answer Prompt"}
              </span>
              <span className="text-xs text-slate-500 font-medium">
                Total in set: <span className="text-orange-600 font-bold">{questionsList.length}</span>
              </span>
            </div>

            {/* Question Text */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Question Prompt
              </label>
              <textarea
                name="question"
                rows="3"
                value={currentQuestion.question}
                onChange={handleQuestionChange}
                placeholder="Enter the question text clearly..."
                className="w-full p-3 bg-slate-50 border border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-200 rounded-xl text-slate-800 text-sm outline-none transition-all"
              />
            </div>

            {/* If MCQ: 4 Options with Radio Selector */}
            {examData.examType === "MCQ" ? (
              <div className="space-y-3 pt-2">
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Options & Correct Answer (Select the radio of the correct option)
                </label>
                {[1, 2, 3, 4].map((num) => {
                  const optKey = `option${num}`;
                  const optVal = currentQuestion[optKey];
                  const isChecked = currentQuestion.correctOption === optVal && optVal.trim() !== "";

                  return (
                    <div
                      key={num}
                      className={`flex items-center gap-3 p-2.5 rounded-xl border transition-all ${
                        isChecked ? "bg-emerald-50 border-emerald-400" : "bg-slate-50 border-slate-200"
                      }`}
                    >
                      <input
                        type="radio"
                        name="correctOptionRadio"
                        checked={isChecked}
                        onChange={() => {
                          if (optVal.trim()) {
                            setCurrentQuestion((prev) => ({ ...prev, correctOption: optVal }));
                          } else {
                            showWarning(`Please type Option ${num} before selecting it as correct`);
                          }
                        }}
                        className="h-4 w-4 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
                      />
                      <span className="text-xs font-bold text-slate-500 w-6">
                        {String.fromCharCode(64 + num)}.
                      </span>
                      <input
                        type="text"
                        name={optKey}
                        value={optVal}
                        onChange={(e) => {
                          const val = e.target.value;
                          setCurrentQuestion((prev) => {
                            const updated = { ...prev, [optKey]: val };
                            if (prev.correctOption === optVal) {
                              updated.correctOption = val;
                            }
                            return updated;
                          });
                        }}
                        placeholder={`Option ${num} text`}
                        className="flex-1 bg-transparent text-sm text-slate-800 outline-none placeholder-slate-400"
                      />
                      {isChecked && (
                        <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-md">
                          Correct Choice
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              /* If Question-Answer: Sample Answer / Rubric */
              <div className="space-y-4 pt-2">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Reference / Ideal Sample Answer (Optional evaluation guidelines)
                  </label>
                  <textarea
                    name="sampleAnswer"
                    rows="3"
                    value={currentQuestion.sampleAnswer}
                    onChange={handleQuestionChange}
                    placeholder="Provide model answer or rubric points..."
                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 text-sm outline-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Marks for this Question
                    </label>
                    <input
                      type="number"
                      name="marks"
                      value={currentQuestion.marks}
                      onChange={handleQuestionChange}
                      min="1"
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Max Word Limit (guideline)
                    </label>
                    <input
                      type="number"
                      name="wordLimit"
                      value={currentQuestion.wordLimit}
                      onChange={handleQuestionChange}
                      min="20"
                      step="50"
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Add to List Button */}
            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={handleAddQuestion}
                className="py-2.5 px-5 bg-slate-900 hover:bg-slate-800 text-white font-medium rounded-xl shadow-sm transition-all flex items-center gap-2 text-sm"
              >
                <Icon name="plus" className="w-4 h-4" />
                Add Question to Exam
              </button>
            </div>
          </div>

          {/* List of Added Questions */}
          {questionsList.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
                Added Questions ({questionsList.length})
              </h3>
              <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                {questionsList.map((q, idx) => (
                  <div
                    key={q.id || idx}
                    className="p-4 bg-white border border-slate-200 rounded-xl shadow-sm space-y-2 relative group"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-start gap-2">
                        <span className="font-bold text-orange-600 text-sm">Q{idx + 1}.</span>
                        <p className="text-sm font-semibold text-slate-800">{q.question}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => handleRemoveQuestion(idx)}
                        className="text-slate-400 hover:text-rose-600 p-1 transition-colors"
                        title="Remove question"
                      >
                        <Icon name="trash" className="w-4 h-4" />
                      </button>
                    </div>

                    {q.questionType === "MCQ" ? (
                      <div className="grid grid-cols-2 gap-2 pt-1 text-xs">
                        {[q.option1, q.option2, q.option3, q.option4].map((opt, oIdx) => {
                          const isCorrect = opt === q.correctOption;
                          return (
                            <div
                              key={oIdx}
                              className={`p-2 rounded-lg border ${
                                isCorrect
                                  ? "bg-emerald-50 border-emerald-300 text-emerald-800 font-semibold"
                                  : "bg-slate-50 border-slate-100 text-slate-600"
                              }`}
                            >
                              <span className="text-slate-400 mr-1.5">{String.fromCharCode(65 + oIdx)}:</span>
                              {opt}
                              {isCorrect && " ✓"}
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100 space-y-1">
                        <p>
                          <span className="font-semibold text-slate-700">Sample Answer:</span>{" "}
                          {q.sampleAnswer || "None specified"}
                        </p>
                        <p className="text-slate-400">
                          Marks: {q.marks} | Word limit: {q.wordLimit} words
                        </p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex items-center justify-between pt-4 border-t border-slate-200">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-xl text-sm transition-all"
            >
              ← Back to Details
            </button>

            <button
              type="button"
              disabled={loading || questionsList.length === 0}
              onClick={handleFinalSubmit}
              className="py-3 px-8 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-bold rounded-xl shadow-md hover:shadow-lg transition-all flex items-center gap-2 text-sm disabled:opacity-50"
            >
              {loading ? "Publishing..." : `Publish Exam (${questionsList.length} Questions) ✓`}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
