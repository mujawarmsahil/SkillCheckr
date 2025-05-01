import React, { useEffect, useState } from "react";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";
import axios from "axios";
import { error } from "jquery";

const TotalUsers = () => {
  const [students, setStudents] = useState([]);

  const [teachers, setTeachers] = useState([]);

  const [effectData, setEffectData] = useState("teachers"); //  which will the first show the pia chart To track the selected data (students/teachers)
  useEffect(() => {
    axios
      .get("http://localhost:8080/api/Admin/viewAllTeacher")
      .then((response) => {
        console.log("Teacher data:", response.data);
        setTeachers(response.data);
      })
      .catch((error) => {
        console.error("error fetching teachers: ", error);
      });
  }, []);

  useEffect(() => {
    axios
      .get("http://localhost:8080/api/Admin/viewAllStudent")
      .then((response) => {
        console.log("Student data:", response.data);
        setStudents(response.data);
      })
      .catch((error) => {
        console.error("error fetching teachers: ", error);
      });
  }, []);

  const handlePieClick = (entry) => {
    if (entry.name === "Teachers") {
      setEffectData("teachers"); // Show teacher data
    } else if (entry.name === "Students") {
      setEffectData("students"); // Show student data
    }
  };

  const handleDeleteStudent = async (id) => {
    // setStudents(students.filter((student) => student.id !== id));
    try {
      const response = await axios.delete(
        `http://localhost:8080/api/Admin/studentDelteteById/${id}`
      );

      if (response.status === 200) {
        alert(" Student Deleted Succefully");
      }
      const updateUsers = students.filter(
        (student) => student.student_id !== id
      );
      setStudents(updateUsers);
    } catch (error) {}
  };

  const handleDeleteTeacher = async (id) => {
    try {
      const response = await axios.delete(
        `http://localhost:8080/api/Admin/teacherDeleteById/${id}`
      );
      if (response.status === 200) {
        alert("Teacher Delete Succufully");
      }
      const updateUsers = teachers.filter(
        (teacher) => teacher.teacher_id !== id
      );
      setTeachers(updateUsers);
    } catch (error) {
      console.log(error);
    }

    // setTeachers(teachers.filter((teacher) => teacher.id !== id));
  };
  const data = [
    { name: "Students", value: students.length },
    { name: "Teachers", value: teachers.length },
  ];
  const COLORS = ["#ff5722", "#78909c"];

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
                    {students.map((student, index) => (
                      <tr key={student.id || index}>
                        <td className="border-2 px-4 py-2">
                          {student.student_id}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {student.student_name}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {student.student_email}
                        </td>
                        <td className="border-2 px-4 py-2">
                          <button
                            onClick={() =>
                              handleDeleteStudent(student.student_id)
                            }
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
                    {teachers.map((teacher, index) => (
                      <tr key={teacher.id || index}>
                        {/* <td className="border-2 px-4 py-2">{teacher.id}</td>
                        <td className="border-2 px-4 py-2">{teacher.name}</td>
                        <td className="border-2 px-4 py-2">{teacher.email}</td> */}

                        <td className="border-2 px-4 py-2">
                          {teacher.teacher_id}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {teacher.teacher_name}
                        </td>
                        <td className="border-2 px-4 py-2">
                          {teacher.teacher_email}
                        </td>

                        <td className="border-2 px-4 py-2">
                          <button
                            // onClick={() => handleDeleteTeacher(teacher.id)}
                            onClick={() =>
                              handleDeleteTeacher(teacher.teacher_id)
                            }
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
