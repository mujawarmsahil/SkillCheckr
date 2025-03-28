import { useState } from "react"

export default function Contact(){
    const[user,setUser] = useState(
        {
            name : "",
            email : "",
            message : "",
        }
    )

    function changeUser(e){
        setUser((prev)=>{
            return {...prev,[e.target.name]:e.target.value}
        })
    }
    return(
        <>
            <section 
                id="contact">
                    <div 
                        className="contact w-full min-h-screen  flex justify-center items-center ">
                            <div 
                                className="w-[80%] min-h-[450px] p-5  flex flex-wrap justify-evenly mt-10">
                                    <div className="w-[100%] p-2 text-center mb-8">
                                    <h1
                                        className="font-intel  inline-block text-4xl relative font-bold text-center after:content[''] after:-bottom-3 after:block after:w-[75px] after:absolute after:left-1/2 after:bg-orange-400  after:-translate-x-1/2 after:h-[5px] ">
                                            Contact
                                    </h1>
                                    </div>
                                <div 
                                    className="locationFrame w-[50%] min-h-[300px] rounded overflow-hidden shadow-lg ">
                                    <iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3784.0665343041205!2d73.8028981!3d18.480645199999998!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3bc2bfeb853d4691%3A0x56f1a2e46627167!2sGiri&#39;s%20TECH%20HUB%20Pvt%20Ltd!5e0!3m2!1sen!2sin!4v1742846485204!5m2!1sen!2sin" 
                                    className="w-[100%] h-[100%]" allowFullScreen={true} loading="lazy" referrerPolicy="no-referrer-when-downgrade"></iframe>
                                </div>
                                <div
                                    className="contactContainer w-[50%] p-2 ">
                                        <form 
                                            className="w-[100%]  "
                                            action="">
                                            <div 
                                                className="formContainer flex flex-col gap-8">
                                                <div 
                                                    className="inputContainer w-[80%] ms-auto h-[50px] relative rounded font-ubuntu ">
                                                    <input
                                                        value={user.name}
                                                        onChange={changeUser}
                                                        className="peer absolute w-full h-full focus:outline-none  focus:ring-2 focus:ring-orange-500 px-3 shadow-[inset_0px_0px_10px_1px_rgba(0,0,0,0.5)] rounded"
                                                        type="text"
                                                        name="name"
                                                        id="name"
                                                    />
                                                    <label
                                                        className={`p-2 absolute left-3  -translate-y-1/2  transition-all peer-placeholder-shown:top-1/2 peer-placeholder-shown:text-gray-500 peer-focus:top-0 peer-focus:text-orange-500 peer-focus:font-semibold z-[10] peer-focus:bg-white ${user.name ? "-top-3 text-black font-semibold" : "top-1/2 text-gray-500"}`}
                                                        htmlFor="name">
                                                        Name
                                                    </label>
                                                </div>

                                                <div 
                                                    className="inputContainer w-[80%] ms-auto h-[50px]  relative rounded font-ubuntu ">
                                                    <input
                                                        value={user.email}
                                                        onChange={changeUser}
                                                        className="peer absolute w-full h-full  focus:outline-none focus:ring-2  focus:ring-orange-500 px-3 rounded shadow-[inset_0px_0px_10px_1px_rgba(0,0,0,0.5)]"
                                                        type="email" 
                                                        name="email" 
                                                        id="email" />
                                                    <label 
                                                        className={`p-2 absolute left-3 -translate-y-1/2  transition-all peer-placeholder-shown:top-1/2 peer-placeholder-shown:text-gray-500 peer-focus:top-0 peer-focus:text-orange-500 peer-focus:font-semibold z-[10] peer-focus:bg-white ${user.email ? "-top-3 text-black font-semibold" : " top-1/2 text-gray-500"}`}
                                                        htmlFor="email">
                                                        Email
                                                    </label>
                                                </div>
                                                <div 
                                                    className="inputContainer w-[80%] ms-auto h-[150px]  relative rounded ">
                                                    <textarea 
                                                        value={user.message}
                                                        onChange={changeUser}
                                                        className="peer absolute w-full h-full  focus:outline-none focus:ring-2  focus:ring-orange-500 p-3 rounded shadow-[inset_0px_0px_10px_1px_rgba(0,0,0,0.5)] resize-none "
                                                        name="message" 
                                                        id="message"
                                                        >
                                                    </textarea>
                                                    <label
                                                        className={`p-2 absolute left-3 -translate-y-1/2  transition-all peer-placeholder-shown:top-1/2 peer-placeholder-shown:text-gray-500 peer-focus:top-0 peer-focus:text-orange-500 peer-focus:font-semibold z-[10] peer-focus:bg-white ${user.message ? "-top-3 text-black font-semibold" : " top-5 text-gray-500"}`}
                                                        htmlFor="message">
                                                        Message
                                                    </label>
                                                </div>
                                                <div
                                                    className="w-[80%] ms-auto">
                                                    <button 
                                                        className="w-full rounded bg-black p-2 text-white"
                                                        type="submit">
                                                        Submit
                                                    </button>
                                                </div>
                                            </div>
                                        </form>
                                </div>
                            </div>

                    </div>
            </section>
        </>
    )
}