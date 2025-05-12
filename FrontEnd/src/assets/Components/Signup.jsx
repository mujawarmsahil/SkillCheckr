import { useState } from "react";
import axios from "axios";

export default function Signup() {
  const [user, setUser] = useState({
    name: "",
    contact: "",
    email: "",
    requested_role: "",
    username: "",
    password: "",
  });

  function changeUserDetails(e) {
    setUser((prev) => {
      return { ...prev, [e.target.name]: e.target.value };
    });
  }
  const [errors, setErrorss] = useState({});

  const handlerSubmit = (e) => {
    e.preventDefault();
    const valiDationatErrors = {};
    if (!user.name.trim()) {
      valiDationatErrors.name = "User Name is required";
    }
    if (!user.email.trim()) {
      valiDationatErrors.email = "Email is required. Please fill it.";
    } else if (!/^[^\s@]{3,}@[^\s@]+\.[^\s@]+$/.test(user.email)) {
      valiDationatErrors.email =
        "Invalid email. Must have at least 3 characters before '@' and be a valid format.";
    }

    if (!user.contact.trim()) {
      valiDationatErrors.contact = "Contact is required";
    } else if (!/^\d{10}$/.test(user.contact)) {
      valiDationatErrors.contact = "minimum 10 digit reqiuerd";
    }

    if (!user.username.trim()) {
      valiDationatErrors.username = "username is required.";
    }

    if (!user.password.trim()) {
      valiDationatErrors.password = "password is required";
    } else if (
      !/(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}/.test(user.password)
    ) {
      valiDationatErrors.password =
        " password must be at least * charaters long and include 1 letter, 1 , and 1 special charater ";
    }

    setErrorss(valiDationatErrors);
    if (Object.keys(valiDationatErrors).length === 0) {
      // axios
        // .post("http://localhost:8080/api/requests/save", user)
        axios
        .post("http://localhost:8080/api/requests/save", user)

        .then((response) => {
          alert("Signup Successfully!"); // meesage Show
          console.log(response.data);
          setUser({
            name: "",
            contact: "",
            email: "",
            requested_role: "",
            username: "",
            password: "",
          });
        })
        .catch((error) => {
          console.error("There was an error submitting the form!", error);
          alert("Something went wrong while submitting!");
        });
      // alert
    }
  };

  return (
    <>
      <form
        onSubmit={handlerSubmit}
        action=""
        className="p-2 bg-transparent mt-5 flex flex-col gap-7"
        method="POST"
      >
        <div className="nameContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto  ">
          <label
            htmlFor="name"
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${
              user?.name
                ? "top-0 bg-white text-orange-400  text-[15px]"
                : "top-1/2 bg-transparent text-slate-600  text-[16px]"
            }`}
          >
            FullName
          </label>
          <input
            onChange={changeUserDetails}
            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 outline-transparent border-2 rounded-md focus:border-orange-400 ${
              user.name
                ? "text-orange-400  "
                : "text-transparent border-transparent "
            }`}
            type="text"
            value={user.name}
            name="name"
            id="name"
          />
          {errors.name && <span className="text-red-700">{errors.name}</span>}
        </div>
        <div className="emailContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
          <label
            htmlFor="email"
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${
              user?.email
                ? "top-0 bg-white text-orange-400  text-[15px]"
                : "top-1/2 bg-transparent text-slate-600 text-[16px]"
            }`}
          >
            Email
          </label>
          <input
            onChange={changeUserDetails}
            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${
              user.email ? "text-orange-400 " : "text-transparent"
            }`}
            type="text"
            value={user.email}
            name="email"
            id="email"
          />
          {errors.email && <span className="text-red-700">{errors.email}</span>}
        </div>
        <div className="contactContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
          <label
            htmlFor=""
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${
              user?.contact
                ? "top-0 bg-white text-orange-400  text-[15px]"
                : "top-1/2 bg-transparent text-slate-600 text-[16px]"
            }`}
          >
            Contact
          </label>
          <input
            onChange={changeUserDetails}
            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent focus:border-orange-400 border-2 ${
              user.contact ? "text-orange-400 " : "text-transparent"
            }`}
            type="text"
            value={user.contact}
            name="contact"
            id="contact"
          />
          {errors.contact && (
            <span className="text-red-700">{errors.contact}</span>
          )}
        </div>
        <div className="userContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
          <label
            htmlFor="username"
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${
              user?.username
                ? "top-0 bg-white text-orange-400  text-[15px]"
                : "top-1/2 bg-transparent text-slate-600 text-[16px]"
            }`}
          >
            Username
          </label>
          <input
            onChange={changeUserDetails}
            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${
              user.username ? "text-orange-400 " : "text-transparent"
            }`}
            type="text"
            value={user.username}
            name="username"
            id="username"
          />
          {errors.username && (
            <span className="text-red-700">{errors.username}</span>
          )}
        </div>
        <div className="passwordContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative  max-w-[80%] mx-auto ">
          <label
            htmlFor="password"
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${
              user?.password
                ? "top-0 bg-white  text-orange-400  text-[15px]"
                : "top-1/2 bg-transparent text-slate-600 text-[16px]"
            }`}
          >
            Password
          </label>
          <input
            onChange={changeUserDetails}
            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${
              user.password ? "text-orange-400 " : "text-transparent"
            }`}
            type="password"
            value={user.password}
            name="password"
            id="password"
          />
          {errors.password && (
            <span className="text-red-700">{errors.password}</span>
          )}
        </div>

        <div className="selectdropdown h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
          <label htmlFor="requested_role" className="text-orange-600">
            Select Role
          </label>

          <select
            name="requested_role"
            id="requested_role"
            value={user.requested_role}
            onChange={changeUserDetails}
            className="ml-6 w-[50%] "
          >
            <option value="select">select</option>
            <option value="Student">Student</option>
            <option value="Teacher">Teacher</option>
          </select>

          {errors.requested_role && (
            <span className="text-red-700">{errors.requested_role}</span>
          )}
        </div>
        <div className="buttonContainer mx-auto">
          <button
            className="px-4  py-2 bg-orange-400 hover:bg-orange-500 rounded text-white"
            type="submit"
          >
            <span className=" drop-shadow-[0_0px_4px_rgba(0,0,0,0.5)]">
              Signup
            </span>
          </button>
        </div>
      </form>
    </>
  );
}
