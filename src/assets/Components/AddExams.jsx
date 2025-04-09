import React, { useState } from "react";
// import { Link } from "react-router-dom";
import { useNavigate } from "react-router-dom";

// import Create_Exams from "./Create_Exams";
// import { useContext } from "react";

export default function AddExams() {
  const [exams, setExams] = useState({
    examName: "",
    subject: "",
    Date: "",
    startTime: "",
    endTime: "",
    totalMarks: 0,
    passingMarks: 0,
  });

  // use the reduxe perform the best add store etc

  function changeExamsDetails(e) {
    const name = e.target.name;
    const value = e.target.value;

    if (name === "endTime" && exams.startTime) {
      if (value <= exams.startTime) {
        alert("End Time must be after Start Time");
        return;
      }
    }

    if (name === "startDate") {
      const today = new Date().toISOString().split("T")[0];
      if (value < today) {
        alert("Start Date should be today or later");
        return;
      }
    }

    setExams((prev) => ({ ...prev, [name]: value }));
  }

  const navigate = useNavigate();

  function handleSubmit(e) {
    e.preventDefault();
    localStorage.setItem("examName", exams.examName);
    navigate("/createExam");
  }

  /*
  async function submitExam() {
    try {
      const response = await fetch("https://your-api-endpoint.com/exams", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(exams),
      });
  
      if (response.ok) {
        alert("Exam created successfully!");
      } else {
        alert("Failed to create exam.");
      }
    } catch (error) {
      console.error("Error creating exam:", error);
      alert("Something went wrong!");
    }
  }
  */

  return (
    <div className="w-full min-h-screen bg-slate-50 p-4">
      <h1 className="text-xl sm:text-2xl text-slate-800 font-semibold mt-4 font-ubuntu text-center py-4 underline">
        📝 Add Exams Component
      </h1>

      <form
        onSubmit={handleSubmit}
        className="flex flex-col items-center gap-6 w-full sm:w-[90%] md:w-[75%] lg:w-[60%] mx-auto bg-white shadow-md rounded-md p-4 md:p-6"
      >
        {/* Exam Name */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="examName"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.examName
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            📝 Exam Name
          </label>
          <input
            id="examName"
            name="examName"
            type="text"
            value={exams.examName}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.examName ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Subject */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="subject"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.subject
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Subject
          </label>
          <input
            id="subject"
            name="subject"
            type="text"
            value={exams.subject}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.subject ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Start Date */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="startDate"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.startDate
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Start Date
          </label>
          <input
            id="startDate"
            name="startDate"
            type="date"
            value={exams.startDate}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.startDate ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Start Time */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="startTime"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.startTime
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Start Time
          </label>
          <input
            id="startTime"
            name="startTime"
            type="time"
            value={exams.startTime}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.startTime ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* End Time */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="endTime"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.endTime
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            End Time
          </label>
          <input
            id="endTime"
            name="endTime"
            type="time"
            value={exams.endTime}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.endTime ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Total Marks */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="totalMarks"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.totalMarks
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Total Marks
          </label>
          <input
            id="totalMarks"
            name="totalMarks"
            type="text"
            value={exams.totalMarks}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.totalMarks ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Passing Marks */}
        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="passingMarks"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.passingMarks
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Passing Marks
          </label>
          <input
            id="passingMarks"
            name="passingMarks"
            type="text"
            value={exams.passingMarks}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.passingMarks ? "text-orange-400" : "text-transparent"
            }`}
          />
        </div>

        {/* Submit Button */}
        <div className="text-center pt-2">
          {/* <Link to="/createExam"> */}
          <button
            type="submit"
            className="bg-slate-800 shadow-xl w-[8rem] sm:w-[10rem] h-[2.5rem] sm:h-[3rem] rounded-2xl font-ubuntu text-white hover:bg-orange-500 hover:shadow-2xl transition-all duration-300"
          >
            Create Exam
          </button>
          {/* </Link> */}
        </div>
      </form>
    </div>
  );
}
