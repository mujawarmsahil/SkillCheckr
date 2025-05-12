// import React from "react";
// import { useState, useEffect } from "react";
// import { useNavigate } from "react-router-dom";

// import axios from "axios";

// export default function RegisterUser() {
//   const [Regiuser, setUsers] = useState([]);
//   const navigate = useNavigate(); // initialize navigation

//   useEffect(() => {
//     fetchRequests();
//   }, []);
//   const fetchRequests = async () => {
//     // if (window.hasFetchedRequests) return; // here we because page two time load thats why we use here window
//     // window.hasFetchedRequests = true;
//     try {
//       const response = await axios.get(
//         "http://localhost:8080/api/requests/viewAllRegisterUsers"
//       );
//       setUsers(response.data);
//     } catch (error) {
//       console.error("Error fetching users:", error);
//       alert("Failed to load registered users.");
//     }
//   };

//   // useEffect(() => {
//   //   fetchRequests();
//   // }, []);

//   // const fetchRequests = async () => {
//   //   //  if (window.hasFetchedRequests) return; // here we because page two time load thats why we use here window
//   //   // window.hasFetchedRequests = true;
//   //   try {
//   //     const response = await axios.get(
//   //       "http://localhost:8080/api/requests/viewAllRegisterUsers"
//   //     );
//   //     setUsers(response.data);
//   //   } catch (error) {
//   //     console.error("Error fetching users:", error);
//   //     alert("Failed to load registered users.");
//   //   }
//   // };

//   const updateRole = async (user, role) => {
//     if (user.status === "Approve") {
//       alert("Data already added.");
//       return;
//     }

//     if (user.requested_role !== role) {
//       alert(
//         `${user.name} requested for ${user.requested_role} role, not ${role}.`
//       );
//       return;
//     }

//     try {
//       let apiUrl = "";
//       if (role === "Teacher") {
//         apiUrl = `http://localhost:8080/api/Admin/addTeacher/${user.request_id}`;
//       } else if (role === "Student") {
//         apiUrl = `http://localhost:8080/api/Admin/addStudent/${user.request_id}`;
//       }

//       const response = await axios.post(apiUrl);
//       if (response.data === false) {
//         alert("data alredy Added");
//         return;
//       }

//       if (response.status === 200) {
//         setUsers(
//           Regiuser.map((u) =>
//             u.request_id === user.request_id ? { ...u, status: "Approve" } : u
//           )
//         );
//         alert(`${role} added successfully`);
//         if (role === "Teacher") navigate("/user/admin");
//         else if (role === "Student") navigate("/user/admin");
//       }
//     } catch (error) {
//       alert(
//         "Something went wrong while updating the user role." +
//           error +
//           " Data Already added yaar "
//       );
//     }
//   };

//   const deleteUser = async (id) => {
//     try {
//       const response = await axios.delete(
//         `http://localhost:8080/api/requests/deleteById/${id}`
//       );
//       if (response.status === 200) {
//         alert("user Delete Succefully");
//         // setUsers(
//         //   RegisterUser,
//         //   filter((user) => user.request_id !== id)
//         // );
//         const updateUsers = Regiuser.filter((user) => user.request_id !== id);
//         setUsers(updateUsers);
//       } else {
//         alert("Failed to delete user.");
//       }
//     } catch (error) {
//       console.error("Error deleting user:", error);
//       alert("Something went wrong while deleting the user.");
//     }
//     // setUsers(Regiuser.filter((user) => user.request_id !== id));
//   };

//   return (
//     <>
//       <div className="w-full min-h-screen border-2 bg-slate-50 ">
//         {/* Thise is a main div measn all */}
//         <div className=" font-bold  p-5 flex underline justify-center ">
//           {" "}
//           <h1 className="text-late-800 text-2xl ">
//             <div>📋 Register Users Component</div>{" "}
//           </h1>
//         </div>

//         <div className="overflow-y-auto max-h-[500px] flex justify-center  w-full">
//           <table className="border-2 w-[90%] text-center bg-white shadow-lg rounded-lg mt-6">
//             <thead className="bg-gray-200">
//               <tr>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%] ">
//                   ID
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Name
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Contact
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Email
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Role
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Status
//                 </th>
//                 <th className="border-2 px-4 py-2 font-ubuntu text-[102%]">
//                   Action
//                 </th>
//               </tr>
//             </thead>
//             <tbody>
//               {Regiuser.map((user) => (
//                 <tr key={user.request_id}>
//                   <td className="border-2 px-4 py-2 ">{user.request_id}</td>
//                   <td className="border-2 px-4 py-2 ">{user.name}</td>
//                   <td className="border-2 px-4 py-2 ">{user.contact}</td>
//                   <td className="border-2 px-4 py-2 ">{user.email}</td>

//                   <td className="border-2 px-4 py-2 ">{user.requested_role}</td>
//                   <td className="border-2 px-4 py-2 ">{user.status}</td>
//                   <td className="border-2 px-4 py-2 ">
//                     <button
//                       className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded text-xs"
//                       onClick={() => updateRole(user, "Teacher")}
//                     >
//                       Add Teacher
//                     </button>

//                     <button
//                       className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded text-xs"
//                       onClick={() => updateRole(user, "Student")}
//                     >
//                       Add Student
//                     </button>

//                     <button
//                       className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded text-xs"
//                       onClick={() => deleteUser(user.request_id)}
//                     >
//                       Delete
//                     </button>
//                   </td>
//                 </tr>
//               ))}
//             </tbody>
//           </table>
//         </div>
//       </div>
//       {/* </div> */}
//     </>
//   );
// }

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

export default function RegisterUser() {
  const [registeredUsers, setRegisteredUsers] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const response = await axios.get(
        "http://localhost:8080/api/requests/viewAllRegisterUsers"
      );
      setRegisteredUsers(response.data);
    } catch (error) {
      console.error("Error fetching users:", error);
      alert("Failed to load registered users.");
    }
  };

  const updateRole = async (user, role) => {
    if (user.status === "Approve") {
      alert("This user is already approved.");
      return;
    }

    if (user.requested_role !== role) {
      alert(
        `${user.name} has requested for the ${user.requested_role} role, not ${role}.`
      );
      return;
    }

    try {
      let apiUrl = "";
      if (role === "Teacher") {
        apiUrl = `http://localhost:8080/api/Admin/addTeacher/${user.request_id}`;
      } else if (role === "Student") {
        apiUrl = `http://localhost:8080/api/Admin/addStudent/${user.request_id}`;
      }

      const response = await axios.post(apiUrl);

      if (response.data === false) {
        alert("User already added.");
        return;
      }

      if (response.status === 200) {
        setRegisteredUsers((prev) =>
          prev.map((u) =>
            u.request_id === user.request_id ? { ...u, status: "Approve" } : u
          )
        );
        alert(`${role} added successfully.`);
        navigate("/user/admin");
      }
    } catch (error) {
      console.error("Error updating role:", error);
      alert("Something went wrong while updating the user role.");
    }
  };

  const deleteUser = async (id) => {
    try {
      const response = await axios.delete(
        `http://localhost:8080/api/requests/deleteById/${id}`
      );
      if (response.status === 200) {
        alert("User deleted successfully.");
        const updatedUsers = registeredUsers.filter(
          (user) => user.request_id !== id
        );
        setRegisteredUsers(updatedUsers);
      } else {
        alert("Failed to delete user.");
      }
    } catch (error) {
      console.error("Error deleting user:", error);
      alert("Something went wrong while deleting the user.");
    }
  };

  return (
    <div className="w-full min-h-screen bg-slate-50">
      <div className="font-bold p-5 flex underline justify-center">
        <h1 className="text-slate-800 text-2xl">📋 Register Users Component</h1>
      </div>

      <div className="overflow-y-auto max-h-[500px] flex justify-center w-full">
        <table className="border-2 w-[90%] text-center bg-white shadow-lg rounded-lg mt-6">
          <thead className="bg-gray-200">
            <tr>
              <th className="border-2 px-4 py-2">ID</th>
              <th className="border-2 px-4 py-2">Name</th>
              <th className="border-2 px-4 py-2">Contact</th>
              <th className="border-2 px-4 py-2">Email</th>
              <th className="border-2 px-4 py-2">Role</th>
              <th className="border-2 px-4 py-2">Status</th>
              <th className="border-2 px-4 py-2">Action</th>
            </tr>
          </thead>
          <tbody>
            {registeredUsers.map((user) => (
              <tr key={user.request_id}>
                <td className="border-2 px-4 py-2">{user.request_id}</td>
                <td className="border-2 px-4 py-2">{user.name}</td>
                <td className="border-2 px-4 py-2">{user.contact}</td>
                <td className="border-2 px-4 py-2">{user.email}</td>
                <td className="border-2 px-4 py-2">{user.requested_role}</td>
                <td className="border-2 px-4 py-2">{user.status}</td>
                <td className="border-2 px-4 py-2 flex flex-col md:flex-row justify-center items-center space-y-1 md:space-y-0 md:space-x-2">
                  <button
                    className="bg-slate-800 hover:bg-sky-600 text-white px-3 py-2 rounded text-xs"
                    onClick={() => updateRole(user, "Teacher")}
                  >
                    Add Teacher
                  </button>
                  <button
                    className="bg-slate-800 hover:bg-sky-600 text-white px-3 py-2 rounded text-xs"
                    onClick={() => updateRole(user, "Student")}
                  >
                    Add Student
                  </button>
                  <button
                    className="bg-red-600 hover:bg-red-700 text-white px-3 py-2 rounded text-xs"
                    onClick={() => deleteUser(user.request_id)}
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
  );
}
