import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

export default function AddExams() {
  const [exams, setExams] = useState({
    examName: "",
    subject: "",
    subject_code: "",
    startDate: "",
    startTime: "",
    endTime: "",
    totalMarks: "",
    passingMarks: "",
  });
  const [subjects, setSubject] = useState([]);
  const navigate = useNavigate();

  const teacherid = localStorage.getItem("teacher_id"); // Assuming teacher_id is stored after login

  console.log("Teacher ID:", teacherid); // Log the teacher_id for debugging
  if (!teacherid) {
    alert("Teacher not logged in or teacher ID not found");
    navigate("/user/teacher"); // Redirect to login page if teacher_id is not found
  }

  function changeExamsDetails(e) {
    const name = e.target.name;

    // Fetch and log user details from localStorage
    const teacher_id = localStorage.getItem("teacher_id");
    const role = localStorage.getItem("role");
    const user_id = localStorage.getItem("user_id");

    console.log("Logged-in User Details:");
    console.log("Teacher ID:", teacher_id);
    console.log("Role:", role);
    console.log("User ID:", user_id);
    const value = e.target.value;
    let newValue = value;
    if (name == "totalMarks" || name == "passingMarks") {
      newValue = parseInt(value) || 0;
    }

    if (name === "endTime" && exams.startTime) {
      const startMinutes = timeToMinutes(exams.startTime);
      const endMinutes = timeToMinutes(value);
      console.log("Start Time:", exams.startTime); // Log start time
      console.log("End Time:", value); // Log end time
      console.log("Start in minutes:", startMinutes);
      console.log("End in minutes:", endMinutes);
      const TotalTime = endMinutes - startMinutes;
      console.log("Total Time:", TotalTime);
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
    setExams((prev) => ({ ...prev, [name]: newValue }));
  }

  function timeToMinutes(time) {
    const [hours, minutes] = time.split(":");
    return parseInt(hours) * 60 + parseInt(minutes);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    for (const key in exams) {
      if (exams[key] === "" || exams[key] === null) {
        alert(`please fill the ${key} field`);
        return;
      }
    }
    if (exams.passingMarks <= 0 || exams.passingMarks > exams.totalMarks) {
      alert(
        "Passing Marks must be greater than 0 and less than or equal to Total Marks"
      );
      return;
    }

    const startMinutes = timeToMinutes(exams.startTime);
    const endMinutes = timeToMinutes(exams.endTime);
    const duration = endMinutes - startMinutes;
    if (duration <= 0) {
      alert("End Time must be after Start Time");
      return;
    }
    const dateTime = `${exams.startDate}T${exams.startTime}`;

    const formattedExam = {
      subject: {
        subjectName: exams.subject,
        subjectCode: exams.subject_code,
      },
      teacher_id: teacherid, // TODO: replace with dynamic teacher_id if needed
      exam_name: exams.examName,
      date: dateTime,
      duration_minuets: duration,
      total_marks: exams.totalMarks,
      passing_marks: exams.passingMarks,
      status: "",
      start_time: exams.startTime, //new Add start tiem and end time
      end_time: exams.endTime,
    };

    try {
      const response = await axios.post(
        // /Backend Connect With FrontEnd
        // "http://localhost:8080/api/Exams/addExams",///...................API Calling the Backend
        "http://localhost:8080/api/Exams/addExams", ///...................API Calling the Backend
        formattedExam,
        // user,
        {
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      if (response.status === 200) {
        // const subject_id = response.data.subject_id;
        console.log("Backend Response Data:", response.data);

        // const subject_id = response.data.subject.subject_id;

        const subject_id = response.data.subjectId;
        if (subject_id) {
          alert("Exams Created Successfully - Subject ID: " + subject_id);
          localStorage.setItem("subject_id", subject_id);
          alert("Exams Created Succefull       " + subject_id);
          navigate("/createExam");
        } else {
          alert("Subject ID not found in response.");
          console.log("Response missing subject_id:", response.data);
        }

        console.log("Subject ID:", subject_id);
        // localStorage.setItem("subject_id", subject_id);
        // navigate("/createExam");
        // console.log("Exam created successfully:", response.data);
      } else {
        alert("Failed to Create Exams ");
      }

      // alert("Exam created successfully!");
    } catch (error) {
      console.error("Error creating exam:", error);
      alert("Something went wrong!");
    }

    localStorage.setItem("examName", exams.examName);
    navigate("/createExam");
  }

  //   const handleSubjectChange = (e) => {

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

        <div className="relative h-[45px] w-full sm:w-[80%] md:w-[70%] lg:w-[60%]">
          <label
            htmlFor="subject_code"
            className={`absolute left-3 transition-all font-ubuntu duration-150 -translate-y-1/2 font-normal ${
              exams.subject_code
                ? "top-0 text-orange-400 text-[15px] bg-white"
                : "top-1/2 text-slate-600 text-[16px] bg-transparent"
            }`}
          >
            Subject code
          </label>
          <input
            id="subject_code"
            name="subject_code"
            type="text"
            value={exams.subject_code}
            onChange={changeExamsDetails}
            className={`w-full h-full font-ubuntu px-4 border-2 rounded-md shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] outline-transparent focus:border-orange-400 ${
              exams.subject_code ? "text-orange-400" : "text-transparent"
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
