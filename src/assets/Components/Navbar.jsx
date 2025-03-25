import { NavLink } from "react-router-dom"
import Logo from "../images/logo/logo.png"
export default function Navbar(){
    return(
        <>
            <header
                id="heading"
                className="w-full h-[85px] bg-transparent transition-all duration-100 flex items-center justify-around fixed top-0 left-0 z-50">
                <div 
                    className="logoContainer">
                    <NavLink 
                        to="">
                        <img 
                            src={Logo}
                            alt="" />
                    </NavLink>
                </div>
                <nav className="w-[60%]">
                    <ul className="flex justify-around w-full">
                    {/* {path:"setExam",text:"Set Exam"},{path:"viewQuestionSet",text:"View Question set"}, */}
                        {
                            [{path:"",text:"Home"},{path:"about",text:"About"},{path:"service",text:"Service"},{path:"contact",text:"Contact"}].map((link,index)=>{
                                return <li key={index}>
                                            <NavLink
                                                
                                                to={link.path}
                                                className={({isActive})=>`font-intel font-bold after:content-[''] after:h-[4px] relative after:absolute after:bottom-[-50%] after:transition-all duration-100 after:duration-100 transition-all after:left-0 ${isActive ? "text-orange-400 after:w-full after:bg-orange-400" : "after:w-0"}`}>
                                                    {link.text}
                                            </NavLink>
                                        </li>
                                
                            })
                        }
                    </ul>
                </nav>
            </header>
        </>
    )
}