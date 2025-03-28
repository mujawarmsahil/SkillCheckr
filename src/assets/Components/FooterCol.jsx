import {Link} from "react-router-dom"
export default function FooterCol({data}){
    return(
        <>
            <div className="md:w-[20%] w-full min-h-[100px] flex flex-col gap-2 p-2 md:border-r-2 border-[rgba(255,255,255,0.5)]">
                {
                    data?.whiteLogo ? <img src={data.whiteLogo} alt="logo" className={`w-[200px] mx-auto  h-[150px]  aspect-square`} /> : 
                    <div className="w-full  flex flex-col gap-4">
                        <ul 
                            className="self-center px-1 flex flex-col gap-5">
                            {
                                data?.links.map((link,index) => (
                                    <li className="" key={index}>
                                        <p className="block">
                                            <Link 
                                                to={link.link} 
                                                className="hover:text-orange-500 flex items-center  text-center gap-2 transition-colors duration-300"
                                            >
                                                    <i className={`${link.icon} ms-5`}></i>
                                                    {link.text}
                                            </Link>
                                        </p>
                                    </li>
                                ))
                            }
                        </ul>
                    </div>
                }
            </div>
        </>
    )
}