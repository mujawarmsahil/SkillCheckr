import React from "react";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";
import { useState, useEffect } from "react";
import axios from "axios";

export default function Tota_Exam() {
  const [UpComingExam, setUpComping] = useState([]);
  const [Completedexams, setCompletedExames] = useState([]);

  const [effectData, setEffectData] = useState("completed");

  useEffect(() => {
    axios
      .get("http://localhost:8080/api/Exams/viewAllUpComingExam")
      .then((response) => {
        console.log("Exams data:", response.data);
        setUpComping(response.data);
      })
      .catch((error) => {
        console.error("Error fetching exams:", error);
      });
  }, []);

  useEffect(() => {
    axios
      .get("http://localhost:8080/api/Exams/viewAllCompletedExam")
      .then((response) => {
        console.log("Completed exams data:", response.data);
        setCompletedExames(response.data);
      })
      .catch((error) => {
        console.error("Error fetching completed exams:", error);
      });
  }, []);

  const handlePieClick = (entry) => {
    if (entry.name === "Completed Exams") {
      setEffectData("completed");
    } else if (entry.name === "Upcoming Exams") {
      setEffectData("upcoming");
    }
  };

  const data = [
    { name: "Completed Exams", value: Completedexams.length },
    { name: "Upcoming Exams", value: UpComingExam.length },
  ];
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
                    </tr>
                  </thead>
                  <tbody>
                    {Completedexams.map((exam) => (
                      <tr key={exam.id}>
                        <td className="border-2 px-4 py-2">{exam.exam_id}</td>
                        <td className="border-2 px-4 py-2">{exam.exam_name}</td>
                        <td className="border-2 px-4 py-2">{exam.date}</td>
                        <td className="border-2 px-4 py-2">
                          {exam.start_time}
                        </td>
                        <td className="border-2 px-4 py-2">{exam.end_time}</td>
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
                    </tr>
                  </thead>
                  <tbody>
                    {UpComingExam.map((Comingexam) => (
                      <tr key={Comingexam.id}>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.exam_id}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.exam_name}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.date}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.start_time}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {Comingexam.end_time}
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
