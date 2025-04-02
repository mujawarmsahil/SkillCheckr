import React from "react";
import { useState } from "react";

// import TsLogo from "../images/logo/Teacher.png";
// import StLogo from "../images/logo/Student.png";
// import { useParams } from "react-router-dom";

export default function RegisterUser() {
  const [Regiuser, setUsers] = useState([
    { id: 0, name: "Ganesh Rathod", role: "Guid" },
    { id: 1, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 2, name: "Mangesh Wagh", role: "Student" },
    { id: 3, name: "Ganesh Rathod", role: "Guid" },
    { id: 4, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 5, name: "Mangesh Wagh", role: "Student" },
    { id: 0, name: "Ganesh Rathod", role: "Guid" },
    { id: 1, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 2, name: "Mangesh Wagh", role: "Student" },
    { id: 3, name: "Ganesh Rathod", role: "Guid" },
    { id: 4, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 5, name: "Mangesh Wagh", role: "Student" },
    { id: 0, name: "Ganesh Rathod", role: "Guid" },
    { id: 1, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 2, name: "Mangesh Wagh", role: "Student" },
    { id: 3, name: "Ganesh Rathod", role: "Guid" },
    { id: 4, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 5, name: "Mangesh Wagh", role: "Student" },
    { id: 0, name: "Ganesh Rathod", role: "Guid" },
    { id: 1, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 2, name: "Mangesh Wagh", role: "Student" },
    { id: 3, name: "Ganesh Rathod", role: "Guid" },
    { id: 4, name: "Sahil Mujawar", role: "Teacher" }, // json form we apply so thats why add
    { id: 5, name: "Mangesh Wagh", role: "Student" },
  ]);

  const updateRole = (id, role) => {
    setUsers(
      Regiuser.map((user) => (user.id === id ? { ...user, role } : user))
    );
  };
  const deleteUser = (id) => {
    setUsers(Regiuser.filter((user) => user.id !== id));
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
            <div>📋 Register Users Component</div>{" "}
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
                  <th className="border-2 px-4 py-2">Student ID</th>
                  <th className="border-2 px-4 py-2">Student Name</th>
                  <th className="border-2 px-4 py-2">Email</th>
                  <th className="border-2 px-4 py-2">Action</th>
                </tr>
              </thead>
              <tbody>
                {Regiuser.map((user) => (
                  <tr key={user.id}>
                    <td className="border-2 px-4 py-2">{user.id}</td>
                    <td className="border-2 px-4 py-2">{user.name}</td>
                    <td className="border-2 px-4 py-2">{user.role}</td>
                    <td className="border-2 px-4 py-2">
                      <button
                        className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded "
                        onClick={() => updateRole(user.id, "Teacher")}
                      >
                        Add Teacher
                      </button>
                      <button
                        className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded "
                        onClick={() => updateRole(user.id, "Student")}
                      >
                        Add Student
                      </button>
                      <button
                        className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded "
                        onClick={() => deleteUser(user.id)}
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
      {/* </div> */}
    </>
  );
}
