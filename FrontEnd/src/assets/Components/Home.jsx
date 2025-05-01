import { useEffect,useRef, useState } from "react"
import {Link}  from "react-router-dom"
import Card from "./Card"
import backgroundImage from "../images/backgroundImages/backgroundImage.jpg"
export default function Home(){
    const [index,setIndex] = useState(0);
    
    let ref = useRef(null)
    useEffect(()=>{
        const slogans = ["Unlock Your Potential, One Skill at a Time!","Know Your Strengths, Conquer Your Goals!","Test Your Skills, Elevate Your Career!","Learn. Improve. Excel.","Assess Your Skills, Track Your Progress!"]

        const updateSlogan = () => {
            if (ref.current) {
                ref.current.style.top = "-100%";
                ref.current.style.opacity = "0"
                setTimeout(() => {
                    if (ref.current) {
                        ref.current.innerHTML = slogans[index % slogans.length];
                        ref.current.style.top = "0";
                        ref.current.style.opacity = "1"

                    }
                }, 2000); 
            }
            setIndex((prev) => prev + 1);
        };

        const timeout = setTimeout(() => {
            updateSlogan();
        }, 10000);

        return () => clearTimeout(timeout); 
    }, [index])

    return(
        <>
            <section 
                id="homeContainer">
                <div 
                    style={{ backgroundImage: `url(${backgroundImage})` }}
                    className={`homeContainer w-full min-h-screen bg-cover bg-no-repeat bg-center flex items-center`}>
                    <div 
                        className="homeInfo w-full md:w-[50%] p-2 ms-auto min-h-full flex flex-col gap-2">

                        <h1
                            className="font-intel font-bold text-[30px] md:text-[50px] text-center md:text-left">
                            Skill 
                            <span
                                className="text-orange-400">
                                Checker
                                </span>
                        </h1>
                        <div 
                            className="slogan w-full md:w-[75%] h-[100px] text-wrap relative overflow-hidden" >
                            <p
                                ref={ref} 
                                className="overflow-hidden transition-all duration-100 font-bold font-ubuntu text-xl md:text-2xl absolute top-0 text-center md:text-left">
                                Assess Your Skills, Track Your Progress!
                            </p>
                            <p
                                className="font-semibold absolute top-1/2 text-center md:text-left">
                                "Take personalized quizzes, evaluate your knowledge, and unlock your potential with our Skill Checker App."
                            </p>
                        </div>
                        <div 
                            className="authContainer mt-2 flex flex-col md:flex-row gap-4 md:gap-20 items-center">
                            {
                                [{text:"Enter as Contender",icon:"fa-solid fa-user-graduate",url:"/authentication"},{text:"Enter as Navigator",icon:"fas fa-chalkboard-teacher",url:"/authentication"}].map((user)=>(
                                    <Link to={user.url} key={user.text}>
                                        <button 
                                            type="button" 
                                            className="bg-orange-400 p-2 rounded hover:bg-orange-500 shadow-lg font-bold text-white w-full md:w-auto">
                                                {user.text}
                                                <i className={`${user.icon} ms-2 border-l-2 p-2 border-l-white`}></i>
                                        </button>
                                    </Link>
                                ))
                            }
                        </div>
                    </div>
                </div>
                <div className="overflow-hidden relative w-full py-10 gap-10 flex flex-col ">
                            <h2
                                className="font-extrabold text-2xl md:text-4xl text-center after:block relative after:absolute after:-bottom-4  after:left-1/2 after:-translate-x-1/2 after:-translate-y-1/2 after:content-[''] after:w-[100px] after:h-[5px] after:bg-orange-400">
                                    Testimonials
                            </h2>
                        <Card/>
                </div>
            </section>
        </>
    )
}

// "Take personalized quizzes, evaluate your knowledge, and unlock your potential with our Skill Checker App."