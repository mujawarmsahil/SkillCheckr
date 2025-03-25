import {Link} from "react-router-dom"
export default function FooterCol({data}){
    return(
        <>
            <div className="w-[20%] min-h-[200px] rounded-lg shadow-lg flex flex-col gap-4 p-4">
                {
                    data?.whiteLogo ? <img src={data.whiteLogo} alt="logo" className={`w-[200px] h-[200px] mx-auto`} /> : 
                    <div className="w-full  flex flex-col gap-4">
                        <h3
                            className="text-2xl font-semibold font-ubuntu italic text-orange-400">
                                {data?.heading}
                        </h3>
                        <ul 
                            className="self-center px-4 flex flex-col gap-5">
                            {
                                data?.links.map((link,index) => (
                                    <li className="mx-auto" key={index}>
                                        <Link 
                                            to={link.link} 
                                            className="hover:text-orange-500 flex flex-col text-center gap-2 transition-colors duration-300"
                                        >
                                            <i 
                                                className={`${link.icon} text-[1.6rem] hover:${link?.color}`}
                                            ></i>
                                            <p className="block">
                                                    {link.text}
                                            </p>
                                        </Link>
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