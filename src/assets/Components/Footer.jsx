import  FooterCol from "./FooterCol"
export default function Footer(){
    const footer = [
        {
            whiteLogo: '../src/assets/images/logo/animatedlogo.gif'
        },
        {
            heading: "Quick Links",
            links: [
                {
                    link: "",
                    icon: "fa-solid fa-house",
                    text: "Home"
                },
                {
                    link: "service",
                    icon: "fa-solid fa-gear",
                    text: "Service"
                },
                {
                    link: "contact",
                    icon: "fa-solid fa-phone",
                    text: "Contact"
                },
                {
                    link: "about",
                    icon: "fa-solid fa-circle-info",
                    text: "About"
                }
            ]
        },
        {
            heading: "Socials",
            links: [
                {
                    link: "https://www.instagram.com",
                    icon: "fa-brands fa-instagram",
                    text: "Instagram"
                },
                {
                    link: "https://www.facebook.com",
                    icon: "fa-brands fa-square-facebook",
                    text : "Facebook"
                },
                {
                    link: "https://www.twitter.com",
                    icon: "fa-brands fa-square-x-twitter",
                    text: "Twitter"
                },
                {
                    link: "https://www.linkedin.com",
                    icon: "fa-brands fa-linkedin",
                    text: "LinkedIn"
                }
            ]
        }
    ]
    return (
        <>
            <footer
                className="border-t-[1px] border-gray-100">
                <div 
                    className="footer w-full min-h-[400px] p-8 flex justify-evenly flex-wrap">
                    {
                        footer.map((data, index) => (
                            <FooterCol key={index} data={data} />
                        ))
                    }
                    <div className="w-[20%] min-h-[200px] rounded-lg shadow-lg flex flex-col gap-4 p-6 items-center">
                        <h3 className="text-lg font-bold">Buy Me a Coffee</h3>
                        <div className="w-full flex justify-center">
                            <a 
                                href="https://www.buymeacoffee.com" 
                                target="_blank" 
                                rel="noopener noreferrer"
                                className="bg-orange-500 text-black px-4 py-2 rounded-full hover:bg-orange-600 transition">
                                ☕ Support
                            </a>
                        </div>
                    </div>
                </div>
                <div className="text-center mt-4 text-sm text-gray-400">
                    © {new Date().getFullYear()} Exam Recommendation System. All rights reserved.
                </div>
            </footer>
        </>
    )
}