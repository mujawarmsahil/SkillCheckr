import React from "react";
import { useParams } from "react-router-dom";

export default function Dashboard() {
    const { role } = useParams(); // Extract role from URL parameters

    const menuItems = {
        admin: [
            { icon: "fas fa-user-plus", text: "Register Users" },
            { icon: "fas fa-users", text: "Total Users" },
            { icon: "fas fa-file-alt", text: "Total Exams" },
            { icon: "fas fa-edit", text: "Edit Exam" },
            { icon: "fas fa-check", text: "Accept Exam" },
            { icon: "fas fa-user-check", text: "Students Registered" },
        ],
        teacher: [
            { icon: "fas fa-file-alt", text: "Total Exams" },
            { icon: "fas fa-file", text: "Single Exam" },
            { icon: "fas fa-users", text: "Students Registered" },
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
                className="flex relative items-center space-x-4 p-2 rounded-md hover:bg-orange-100 transition-colors duration-300"
            >
                <i className={`${item.icon} text-orange-400 text-lg`}></i>
                <span
                    className="absolute left-8 w-40 h-8 flex items-center justify-start text-start  text-gray-800 group-hover:opacity-100 font-medium transition-opacity duration-300"
                >
                    {item.text}
                </span>
            </li>
        ));

    return (
        <div className="flex h-screen bg-gray-100">
            <div className="group w-16  hover:w-64 transition-all duration-300 ease-in-out bg-white shadow-lg p-4 backdrop-blur-md bg-opacity-70 overflow-hidden flex flex-col">
                <h2 className="text-xl font-bold mb-4 text-orange-400 opacity-0 group-hover:opacity-100 transition-opacity duration-300 whitespace-nowrap">
                    Menu
                </h2>
                <ul className="space-y-3 overflow-hidden">
                    {role === "admin" && renderMenuItems(menuItems.admin)}
                    {role === "teacher" && renderMenuItems(menuItems.teacher)}
                    {role === "student" && renderMenuItems(menuItems.student)}
                </ul>
            </div>
            <div className="flex-1 p-6">
                <h1 className="text-2xl font-bold text-gray-800">Welcome to the Dashboard</h1>
                <p className="text-gray-600 mt-2">Role: {role}</p>
                {/* Add dynamic content here based on the role */}
            </div>
        </div>
    );
}