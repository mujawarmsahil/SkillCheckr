import { useState } from "react";
export default function Login() {
  const [user, setUser] = useState({
    username: "",
    password: "",
  });

  function changeUserDetails(e) {
    setUser((prev) => {
      return { ...prev, [e.target.name]: e.target.value };
    });
  }

  const [errorss, setErrors] = useState("");
  const changeHandler = (e) => {
    e.preventDefault();
    const valiDationErrors = {};
    if (!user.username.trim()) {
      valiDationErrors.username = "User name is Required";
    }
    if (!user.password.trim()) {
      valiDationErrors.password = "password is the Required";
    } else if (!/(?=.*[A-Za-z])(?=.*[^A-Za-z\d]).{8}/.test(user.password)) {
      valiDationErrors.password =
        "must be charaters and 1 digit and $% spesical charaters ";
    }
    setErrors(valiDationErrors);
    if (Object.keys(valiDationErrors).length === 0) {
      alert("form sumbmited succefully ");
    }
  };
  return (
    <>
      <form
        onSubmit={changeHandler}
        action=""
        className="p-3 bg-transparent mt-5 flex flex-col gap-8"
        method="POST"
      >
        <div className="userContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
          <label
            htmlFor="username"
            className={`label font-ubuntu absolute  transition-all duration-150 left-3   -translate-y-1/2  ${
              user?.username
                ? "top-0 bg-white text-orange-400 font-semibold text-[15px]"
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
          {errorss.username && (
            <span className="text-red-400">{errorss.username}</span>
          )}
        </div>
        <div className="passwordContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative  max-w-[80%] mx-auto ">
          <label
            htmlFor="password"
            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2  ${
              user?.password
                ? "top-0 bg-white  text-orange-400 font-semibold text-[15px]"
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
          <span className="text-red-400">{errorss.password}</span>
        </div>
        <div className="buttonContainer mx-auto">
          <button
            className="px-4 py-2 bg-orange-400 rounded text-white"
            type="submit"
          >
            Login
          </button>
        </div>
      </form>
    </>
  );
}
