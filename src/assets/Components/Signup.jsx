import { useState } from "react"

export default function Signup(){
    const [user,setUser] = useState({
        name : "",
        email : "",
        contact : "",
        username : "",
        password : "",
    })

    function changeUserDetails(e){
        setUser((prev)=>{
            return {...prev,[e.target.name] : e.target.value}
        })
    }
    return(
        <>
            <form 
                action=""
                className="p-2 bg-transparent mt-5 flex flex-col gap-7" 
                method="POST">
                <div 
                    className="nameContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto  ">
                        <label 
                            htmlFor="name" 
                            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${user?.name ? "top-0 bg-white text-orange-400  text-[15px]" : "top-1/2 bg-transparent text-slate-600  text-[16px]" }`}>
                                FullName
                        </label>
                        <input
                            onChange={changeUserDetails}
                            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 outline-transparent border-2 rounded-md focus:border-orange-400 ${user.name ? "text-orange-400  " : "text-transparent border-transparent "}`}
                            type="text"
                            value={user.name} 
                            name="name" 
                            id="name" />
                </div>
                <div 
                    className="emailContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
                        <label 
                            htmlFor="email" 
                            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${user?.email ? "top-0 bg-white text-orange-400  text-[15px]" : "top-1/2 bg-transparent text-slate-600 text-[16px]" }`}>
                                Email
                        </label>
                        <input
                            onChange={changeUserDetails}
                            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${user.email ? "text-orange-400 " : "text-transparent"}`}
                            type="text"
                            value={user.email} 
                            name="email" 
                            id="email" />
                </div>
                <div 
                    className="contactContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
                        <label 
                            htmlFor="" 
                            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${user?.contact ? "top-0 bg-white text-orange-400  text-[15px]" : "top-1/2 bg-transparent text-slate-600 text-[16px]" }`}>
                                Contact
                        </label>
                        <input
                            onChange={changeUserDetails}
                            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent focus:border-orange-400 border-2 ${user.contact ? "text-orange-400 " : "text-transparent"}`}
                            type="text"
                            value={user.contact} 
                            name="contact" 
                            id="contact" />
                </div>
                <div 
                    className="userContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative max-w-[80%] mx-auto ">
                        <label 
                            htmlFor="username" 
                            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${user?.username ? "top-0 bg-white text-orange-400  text-[15px]" : "top-1/2 bg-transparent text-slate-600 text-[16px]" }`}>
                                Username
                        </label>
                        <input
                            onChange={changeUserDetails}
                            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${user.username ? "text-orange-400 " : "text-transparent"}`}
                            type="text"
                            value={user.username} 
                            name="username" 
                            id="username" />
                </div>
                <div 
                    className="passwordContainer h-[45px] min-w-[290px] w-[400px] bg-transparent relative  max-w-[80%] mx-auto ">
                        <label 
                            htmlFor="password" 
                            className={`label absolute font-ubuntu  transition-all duration-150 left-3   -translate-y-1/2 font-normal  ${user?.password ? "top-0 bg-white  text-orange-400  text-[15px]" : "top-1/2 bg-transparent text-slate-600 text-[16px]" }`}>
                                Password
                        </label>
                        <input
                            onChange={changeUserDetails}
                            className={`w-full h-full font-ubuntu shadow-[inset_0_0_10px_1px_rgba(0,0,0,0.2)]  px-4 rounded-md outline-transparent border-2 focus:border-orange-400 ${user.password ? "text-orange-400 " : "text-transparent"}`}
                            type="password"
                            value={user.password} 
                            name="password" 
                            id="password" />
                </div>
                <div 
                        className="buttonContainer mx-auto">
                            <button 
                                className="px-4  py-2 bg-orange-400 hover:bg-orange-500 rounded text-white"
                                type="submit">
                                    <span 
                                        className=" drop-shadow-[0_0px_4px_rgba(0,0,0,0.5)]">Signup</span>
                            </button>
                    </div>
            </form>
        </>
    )
}