import React, { useState } from "react";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";

const TotalUsers = () => {
  const data = [
    { name: "Students", value: 90 },
    { name: "Teachers", value: 10 },
  ];

  const COLORS = ["#ff5722", "#78909c"];

  const [students, setStudents] = useState([
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    // ...other student entries
  ]);

  const [teachers, setTeachers] = useState([
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    { id: 0, name: "Ganesh Rathod", email: "GaneshDada.com" },
    { id: 1, name: "Sahil Mujawar", email: "Mujawar.com" },
    { id: 2, name: "Mangesh Wagh", email: "mangeshwagh65@gmail.com" },
    { id: 3, name: "Veronika Patil", email: "Veropatil34.com" },
    // ...other teacher entries
  ]);

  const [effectData, setEffectData] = useState("teachers"); // To track the selected data (students/teachers)

  const handlePieClick = (entry) => {
    if (entry.name === "Teachers") {
      setEffectData("teachers"); // Show teacher data
    } else if (entry.name === "Students") {
      setEffectData("students"); // Show student data
    }
  };

  // Fetch Students from API
  // useEffect(() => {
  //   fetch("http://localhost:5000/students")
  //     .then((response) => response.json())
  //     .then((data) => setStudents(data))
  //     .catch((error) => console.error("Error fetching students:", error));
  // }, []);

  const handleDeleteStudent = (id) => {
    // API DELETE request
    // fetch(`http://localhost:5000/students/${id}`, {
    //   method: "DELETE",
    // })
    //   .then((response) => {
    //     if (response.ok) {
    //       setStudents(students.filter((student) => student.id !== id));
    //     }
    //   })
    //   .catch((error) => console.error("Error deleting student:", error));
    setStudents(students.filter((student) => student.id !== id));
  };

  // Fetch Teachers from API
  // useEffect(() => {
  //   fetch("http://localhost:5000/teachers")
  //     .then((response) => response.json())
  //     .then((data) => setTeachers(data))
  //     .catch((error) => console.error("Error fetching teachers:", error));
  // }, []);

  const handleDeleteTeacher = (id) => {
    // API DELETE request
    // fetch(`http://localhost:5000/teachers/${id}`, {
    //   method: "DELETE",
    // })
    //   .then((response) => {
    //     if (response.ok) {
    //       setTeachers(teachers.filter((teacher) => teacher.id !== id));
    //     }
    //   })
    //   .catch((error) => console.error("Error deleting teacher:", error));
    setTeachers(teachers.filter((teacher) => teacher.id !== id));
  };

  return (
    <div className="w-full min-h-screen flex flex-col items-center bg-slate-50 py-10">
      <h1 className="text-2xl font-bold p-3 font-ubuntu text-slate-800 underline">
        👥 Total Users Component
      </h1>

      <div className="w-full flex flex-col lg:flex-row items-center justify-center mt-10 gap-10">
        {/* Pie Chart */}
        <div className="flex justify-center items-center w-full lg:w-1/3">
          <PieChart width={500} height={400}>
            <Pie
              onClick={handlePieClick}
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

        {/* Tables Section */}
        <div className="w-full lg:w-2/3">
          {effectData === "students" && (
            <div className="flex flex-col items-center">
              <h1 className="text-xl font-bold text-orange-600 flex items-center">
                Students
              </h1>

              <div className="overflow-y-auto max-h-[300px] w-full">
                <table className="border-2 w-full text-center bg-white shadow-lg rounded-lg mt-6">
                  <thead className="bg-gray-200">
                    <tr>
                      <th className="border-2 px-4 py-2">Student ID</th>
                      <th className="border-2 px-4 py-2">Student Name</th>
                      <th className="border-2 px-4 py-2">Email</th>
                      <th className="border-2 px-4 py-2">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.map((student) => (
                      <tr key={student.id}>
                        <td className="border-2 px-4 py-2">{student.id}</td>
                        <td className="border-2 px-4 py-2">{student.name}</td>
                        <td className="border-2 px-4 py-2">{student.email}</td>
                        <td className="border-2 px-4 py-2">
                          <button
                            onClick={() => handleDeleteStudent(student.id)}
                            className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-1 rounded"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {effectData === "teachers" && (
            <div className="flex flex-col items-center mt-5">
              <h1 className="text-xl font-bold text-orange-600 flex items-center">
                Teachers
              </h1>

              <div className="overflow-y-auto max-h-[300px] w-full">
                <table className="border-2 w-full text-center bg-white shadow-lg rounded-lg mt-6">
                  <thead className="bg-gray-200">
                    <tr>
                      <th className="border-2 px-4 py-2">Teacher ID</th>
                      <th className="border-2 px-4 py-2">Teacher Name</th>
                      <th className="border-2 px-4 py-2">Email</th>
                      <th className="border-2 px-4 py-2">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {teachers.map((teacher) => (
                      <tr key={teacher.id}>
                        <td className="border-2 px-4 py-2">{teacher.id}</td>
                        <td className="border-2 px-4 py-2">{teacher.name}</td>
                        <td className="border-2 px-4 py-2">{teacher.email}</td>
                        <td className="border-2 px-4 py-2">
                          <button
                            onClick={() => handleDeleteTeacher(teacher.id)}
                            className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-1 rounded"
                          >
                            Delete
                          </button>
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
};

export default TotalUsers;
