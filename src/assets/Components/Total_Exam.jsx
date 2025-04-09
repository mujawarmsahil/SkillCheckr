import React from "react";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";
import { useState } from "react";

export default function Tota_Exam() {
  const data = [
    { name: "Completed Exams", value: 60 },
    { name: "Upcoming Exams", value: 35 },
  ];

  const [Completedexams, setCompletedExames] = useState([
    {
      id: 1,
      name: "BCA DEC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:30 AM",
      type: "MCQ",
    },
    {
      id: 2,
      name: "MCA DEC",
      date: "2023-11-15",
      ExamsStart: "09:00 AM",
      EndExams: "10:30 AM",
      type: "Written",
    },

    {
      id: 4,
      name: "MOCK TEST BE",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:30 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 5,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
  ]);

  const [UpComingExam, setUpComping] = useState([
    {
      id: 6,
      name: "MOC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "12:00 AM",
      type: "MCQ",
    },
    {
      id: 7,
      name: "MTECH DEC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:00 AM",
      type: "MCQ",
    },
    {
      id: 8,
      name: "MSC JAN",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:00 AM",
      type: "MCQ",
    },
    {
      id: 9,
      name: "MSC FEB",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:00 AM",
      type: "MCQ",
    },
  ]);

  const [effectData, setEffectData] = useState("completed");

  const handlePieClick = (entry) => {
    if (entry.name === "Completed Exams") {
      setEffectData("completed");
    } else if (entry.name === "Upcoming Exams") {
      setEffectData("upcoming");
    }
  };

  const COLORS = ["#ea580c", "#010f21"];

  return (
    <div className="w-full min-h-screen bg-slate-50 py-10 flex flex-col items-center ">
      <h1 className="text-2xl font-bold p-1 font-ubuntu text-slate-800  underline">
        📑 Total exams
      </h1>
      <div className="w-full flex flex-col lg:flex-row items-center justify-center min-h-[600px] gap-5">
        <div className=" flex justify-center items-center w-full lg:w-1/3">
          <PieChart width={500} height={400}>
            <Pie
              onClick={(event, index) => handlePieClick(data[index])}
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={90}
              outerRadius={160}
              fill="#8884d8"
              dataKey="value"
              label
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </div>

        <div className="w-full lg:w-2/3">
          {effectData === "completed" && (
            <div className="w-full flex flex-col items-center">
              <h1 className="text-3xl font-bold font-ubuntu text-orange-600 flex items-center">
                📑 Completed Exams
              </h1>
              <div className="overflow-y-auto max-h-[300px]  w-[99%]  ">
                <table className="border-2 w-full text-center bg-white shadow-lg rounded-lg mt-6 sticky top-0 z-10">
                  <thead className="bg-gray-200 ">
                    <tr>
                      <th className="border px-4 py-2">ID</th>
                      <th className="border px-4 py-2">Exam Name</th>
                      <th className="border px-4 py-2">Exams Date</th>
                      <th className="border px-4 py-2">Exam Start</th>
                      <th className="border px-4 py-2">Exam End</th>
                      <th className="border px-4 py-2">Exam Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Completedexams.map((exam) => (
                      <tr key={exam.id}>
                        <td className="border-2 px-4 py-2">{exam.id}</td>
                        <td className="border-2 px-4 py-2">{exam.name}</td>
                        <td className="border-2 px-4 py-2">{exam.date}</td>
                        <td className="border-2 px-4 py-2">
                          {exam.ExamsStart}
                        </td>
                        <td className="border-2 px-4 py-2">{exam.EndExams}</td>
                        <td className="border-2 px-4 py-2">{exam.type}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {effectData === "upcoming" && (
            <div className=" flex flex-col items-center mt-5">
              <h1 className="text-3xl font-bold flex items-center text-orange-600 ">
                📑 Upcoming Exams
              </h1>
              <div className="overflow-y-auto max-h-[300px] w-[99%]">
                <table className="border-2 w-full text-center bg-white shadow-lg rounded-lg mt-6">
                  <thead className="bg-gray-200">
                    <tr>
                      <th className="border px-4 py-2">Sr</th>
                      <th className="border px-4 py-2">Exam Name</th>
                      <th className="border px-4 py-2">Coming Exams Date</th>
                      <th className="border px-4 py-2">Exam Start</th>
                      <th className="border px-4 py-2">Exam End</th>
                      <th className="border px-4 py-2">Exam Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Completedexams.map((Comingexam) => (
                      <tr key={Comingexam.id}>
                        <td className="border-2 px-4 py-2">{Comingexam.id}</td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.name}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.date}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.ExamsStart}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.EndExams}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.type}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
