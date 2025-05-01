// import TsLogo from "../images/logo/Teacher.png";
// import StLogo from "../images/logo/Student.png";
// import { useParams } from "react-router-dom";

import React from "react";
import { useState } from "react";
export default function AcceptExame() {
 
  const [actionExam, setExamsStatus] = useState([
    {
      id: 0,
      name: "BA",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:30 AM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 1,
      name: "MA",
      date: "2023-10-02",
      ExamsStart: "11:00 AM",
      EndExams: "12:30 PM",
      type: "Essay",
      Status: "",
    },
    {
      id: 2,
      name: "BBA",
      date: "2023-10-03",
      ExamsStart: "12:00 PM",
      EndExams: "1:30 PM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 3,
      name: "MCA",
      date: "2023-10-04",
      ExamsStart: "2:00 PM",
      EndExams: "3:30 PM",
      type: "Practical",
      Status: "",
    },
    {
      id: 4,
      name: "MSC",
      date: "2023-10-05",
      ExamsStart: "3:00 PM",
      EndExams: "4:30 PM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 5,
      name: "MTECH",
      date: "2023-10-06",
      ExamsStart: "4:00 PM",
      EndExams: "5:30 PM",
      type: "Essay",
      Status: "",
    },
    {
      id: 6,
      name: "BE",
      date: "2023-10-07",
      ExamsStart: "9:00 AM",
      EndExams: "10:30 AM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 7,
      name: "MTECH DEC",
      date: "2023-10-01",
      ExamsStart: "10:00 AM",
      EndExams: "11:00 AM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 8,
      name: "All Faculty",
      date: "2023-10-09",
      ExamsStart: "1:00 PM",
      EndExams: "2:30 PM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 9,
      name: "MBA",
      date: "2023-10-10",
      ExamsStart: "2:30 PM",
      EndExams: "4:00 PM",
      type: "Essay",
      Status: "",
    },
    {
      id: 10,
      name: "Engineering",
      date: "2023-10-11",
      ExamsStart: "4:30 PM",
      EndExams: "6:00 PM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 11,
      name: "PhD",
      date: "2023-10-12",
      ExamsStart: "9:00 AM",
      EndExams: "10:30 AM",
      type: "Practical",
      Status: "",
    },
    {
      id: 12,
      name: "BSc",
      date: "2023-10-13",
      ExamsStart: "10:30 AM",
      EndExams: "12:00 PM",
      type: "MCQ",
      Status: "",
    },
    {
      id: 13,
      name: "BCom",
      date: "2023-10-14",
      ExamsStart: "1:00 PM",
      EndExams: "2:30 PM",
      type: "Essay",
      Status: "",
    },
    {
      id: 14,
      name: "MPhil",
      date: "2023-10-15",
      ExamsStart: "2:30 PM",
      EndExams: "4:00 PM",
      type: "MCQ",
      Status: "",
    },
  ]);

  const uodateStatus = (id, status) => {
    setExamsStatus(
      actionExam.map((Exams) =>
        Exams.id === id ? { ...Exams, Status: status } : Exams
      )
    );
  };
  const deleteExams = (id) => {
    setExamsStatus(actionExam.filter((Exams) => Exams.id !== id));
  };

  return (
    <>
      <div className="w-full min-h-screen border-2 bg-slate-50 ">
        {/* Thise is a main div measn all */}
        <div className=" font-bold  p-5 flex underline justify-center ">
          {" "}
          {/* <img
            src={StLogo}
            alt="images Loading "
            className="w-14 pt-1 py-1 mr-10 "
          /> */}
          <h1 className="text-late-800 text-2xl ">
            <div>✅ Accept Exam Component</div>
          </h1>
          {/* <img
            src={TsLogo}
            alt="images Loading "
            className="w-14 pt-1 py-1 ml-10 "
          /> */}
        </div>
        {/* <div className="w-full   flex items-center justify-center"> */}
        {/* Tables */}
        <div className="overflow-y-auto max-h-[500px] flex justify-center  w-full">
          <table className="border-2 w-[90%] text-center bg-white shadow-lg rounded-lg mt-6">
            <thead className="bg-gray-200">
              <tr>
                <th className="border px-4 py-2">ID</th>
                <th className="border px-4 py-2">Exam Name</th>
                <th className="border px-4 py-2">Exams Date</th>
                <th className="border px-4 py-2">Exam Type</th>
                <th className="border px-4 py-2">Exam Start</th>
                <th className="border px-4 py-2">Exam End</th>
                <th className="border px-4 py-2">Status</th>

                <th className="border px-4 py-2">Action</th>
              </tr>
            </thead>
            <tbody>
              {actionExam.map((Exams) => (
                <tr key={Exams.id}>
                  <td className="border-2 px-4 py-2">{Exams.id}</td>
                  <td className="border-2 px-4 py-2">{Exams.name}</td>
                  <td className="border-2 px-4 py-2">{Exams.date}</td>
                  <td className="border-2 px-4 py-2">{Exams.type}</td>
                  <td className="border-2 px-4 py-2">{Exams.ExamsStart}</td>
                  <td className="border-2 px-4 py-2">{Exams.EndExams}</td>
                  <td className="border-2 px-4 py-2">{Exams.Status}</td>
                  {/* <td className="border-2 px-4 py-2">{Exams.status}</td> */}
                  <td className="border-2 px-4 py-2">
                    <button
                      className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded "
                      onClick={() => uodateStatus(Exams.id, "Accepted")}
                    >
                      Accept Exam
                    </button>

                    <button
                      className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded "
                      onClick={() => deleteExams(Exams.id)}
                    >
                      Delete Exam
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {/* </div> */}
    </>
  );
}
