import React, { useState } from "react";
import { useParams } from "react-router-dom";
import Welcome from "./Welcome";
import Result from "./Result";
import RegisterUser from "./RegisterUser";
import TotalUsers from "./TotalUsers";
import Tota_Exam from "./Total_Exam";
import AcceptExame from "./AcceptExame";
import SingleExams from "./SingleExams";
import AddExams from "./AddExams";
// import StudentExams from "./StudentExams";
import Exam from "./Exam";

export default function Dashboard() {
  const { role } = useParams();
  const [activeContent, setActiveContent] = useState(
    "Welcome to the Dashboard"
  );

  const menuItems = {
    admin: [
      { icon: "fas fa-user-plus", text: "Register Users" },
      { icon: "fas fa-users", text: "Total Users" },
      { icon: "fas fa-file-alt", text: "Total Exams" },
      { icon: "fas fa-check", text: "Accept Exam" },
    ],
    teacher: [
      { icon: "fas fa-file-alt", text: "Total Exams" },
      { icon: "fas fa-file", text: "Single Exam" },
      { icon: "fas fa-plus", text: "Add Exam" },
    ],
    student: [
      { icon: "fas fa-book", text: "Exams" },
      { icon: "fas fa-chart-bar", text: "Results" },
    ],
  };

  const renderMenuItems = (items) =>
    items.map((item, index) => (
      <li
        key={index}
        className="flex relative items-center space-x-4 p-2 rounded-md hover:bg-orange-100 transition-colors duration-300 cursor-pointer"
        onClick={() => setActiveContent(item.text)}
      >
        <i className={`${item.icon} text-orange-400 text-lg`}></i>
        <span className="absolute left-8 w-40 h-8 flex items-center justify-start text-gray-800 font-medium transition-opacity duration-300">
          {item.text}
        </span>
      </li>
    ));

  const renderActiveContent = () => {
    switch (activeContent) {
      case "Register Users":
        return <RegisterUser />;
      case "Total Users":
        return <TotalUsers />;
      case "Total Exams":
        return <Tota_Exam />;
      case "Accept Exam":
        return <AcceptExame />;
      case "Single Exam":
        return <SingleExams />;
      case "Add Exam":
        return <AddExams />;
      case "Exams":
        return <Exam />;
      case "Results":
        return (
          <>
            <div>📊 Results Component</div>
            <Result />
          </>
        );
      default:
        return <Welcome role={role} />;
    }
  };

  return (
    <div className="flex h-screen bg-gray-100">
      <div className="group w-16 hover:w-64 transition-all duration-300 ease-in-out bg-white shadow-lg p-4 overflow-hidden flex flex-col">
        <h2 className="text-xl font-bold mb-4 text-orange-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300 whitespace-nowrap">
          Menu
        </h2>
        <ul className="space-y-3">
          {role === "admin" && renderMenuItems(menuItems.admin)}
          {role === "teacher" && renderMenuItems(menuItems.teacher)}
          {role === "student" && renderMenuItems(menuItems.student)}
        </ul>
      </div>

      <div className="flex-1 p-6">
        <h1 className="text-2xl font-bold text-gray-800">Dashboard</h1>
        <p className="text-gray-600 mt-2">Role: {role}</p>
        <div className="mt-4 p-4 bg-white rounded-lg shadow-md">
          {renderActiveContent()}
        </div>
      </div>
    </div>
  );
}
