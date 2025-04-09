import { useState } from "react";
import Logo from "../images/logo/favicon.png";
import Logout from "./Login";
import Signup from "./Signup";
export default function Authentication() {
  const [toggler, setToggler] = useState(false);
  return (
    <>
      <section id="authContainer">
        <div className="authContainer relative w-full min-h-screen border-2 border-red-400 bg-[rgba(209,213,219,0.1)]">
          <div className="container absolute top-1/2 left-1/2 -translate-x-1/2  -translate-y-1/2 border-black max-w-[400px] min-w-[40%] min-h-[300px] p-5 pt-9 rounded-lg shadow-[0_0_10px_1px_rgba(0,0,0,0.3)]">
            <div className="logoContainer absolute -top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 p-1 bg-white shadow-[inset_0_0_5px_1px_rgba(0,0,0,0.3)] object-fill rounded-full">
              <img
                src={Logo}
                className="w-[50px] object-fill h-[50px]"
                alt="logo"
              />
            </div>
            <div className="colContainer w-full h-full ">
              <div className="toggler shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)] bg-[rgba(156,163,175,0.4)] flex justify-around rounded-md h-[35px] py-1 px-1">
                <div className="relative text-center w-1/2">
                  <button
                    onClick={() => setToggler((prev) => !prev)}
                    className={`font-semibold font-intel relative z-50 ${
                      !toggler ? "text-orange-400 " : "text-black "
                    }`}
                  >
                    Signup
                  </button>
                  <div
                    className={`absolute top-0  w-full transition-all duration-150 rounded h-full  z-10 ${
                      !toggler ? "bg-white left-0" : "bg-transparent left-full "
                    }`}
                  ></div>
                </div>
                <div className="relative text-center w-1/2">
                  <button
                    onClick={() => setToggler((prev) => !prev)}
                    className={`font-semibold font-intel relative z-50 ${
                      toggler ? "text-orange-400 " : "text-black "
                    }`}
                  >
                    Login
                  </button>
                  <div
                    className={`absolute top-0 transition-all duration-150 w-full rounded h-full  z-10 ${
                      toggler
                        ? "bg-white left-0 "
                        : "bg-transparent -left-full "
                    }`}
                  ></div>
                </div>
              </div>
              <div className="formContainer bg-transparent">
                {toggler ? <Logout /> : <Signup />}
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
