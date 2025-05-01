import React from "react";

export default function Result() {
  const studentData = [
    {
      studentId: 1,
      subjectId: 101,
      subjectName: "Math",
      marksObtained: 75,
      totalMarks: 100,
      status: "Pass",
    },
    {
      studentId: 2,
      subjectId: 101,
      subjectName: "Java",
      marksObtained: 45,
      totalMarks: 100,
      status: "Fail",
    },
    {
      studentId: 3,
      subjectId: 102,
      subjectName: ".Net",
      marksObtained: 85,
      totalMarks: 100,
      status: "Pass",
    },
  ];
  const getStatusColor = (status) => {
    return status === "Pass" ? "text-green-500" : "text-red-500";
  };

  return (
    <div className="font-ubuntu bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 p-6 rounded-lg shadow-lg text-white max-w-4xl mx-auto">
      <table className="table-auto w-full text-left border-collapse">
        <thead>
          <tr className="bg-purple-700">
            <th className="px-4 py-2">Student ID</th>
            <th className="px-4 py-2">Subject ID</th>
            <th className="px-4 py-2">Subject Name</th>

            <th className="px-4 py-2">Marks Obtained</th>
            <th className="px-4 py-2">Total Marks</th>
            <th className="px-4 py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {studentData.map((data, index) => (
            <tr
              key={index}
              className={`${
                index % 2 === 0 ? "bg-purple-600" : "bg-purple-500"
              }`}
            >
              <td className="px-4 py-2">{data.studentId}</td>
              <td className="px-4 py-2">{data.subjectId}</td>
              <td className="px-4 py-2">{data.subjectName}</td>

              <td className="px-4 py-2">{data.marksObtained}</td>
              <td className="px-4 py-2">{data.totalMarks}</td>
              <td
                className={`px-4 py-2 font-bold ${getStatusColor(data.status)}`}
              >
                {data.status}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
