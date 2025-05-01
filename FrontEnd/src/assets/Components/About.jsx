import History from "./History";
import Values from "./Values";

export default function About(){
    return(
        <>
            <section 
                id="about">
                <div 
                    className="py-12 about w-full min-h-screen ">
                        <History/>
                        <Values/>
                </div>
            </section>
        </>
    )
}