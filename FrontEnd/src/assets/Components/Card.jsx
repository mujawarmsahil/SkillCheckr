import Review from "./Review"
export default function Card(){
    const reviews = [
        {
            heading: "Efficient Assessment Solution!",
            description:"Skill Checkr streamlined our recruitment process by providing reliable assessments and detailed performance analytics. It saved us hours of manual evaluation and ensured we hired the best talent.",
            name: "Anita Sharma, HR Manager, TechNova Solutions",
            image: "../src/assets/images/reviews/anita_sharma.jpg"
        },
        {
            heading: "Accurate Skill Evaluation!",
            description:"We partnered with Skill Checkr for our employee training programs. The platform’s customizable assessments helped us evaluate employee progress effectively. The insights provided were invaluable.",
            name: "Rajesh Verma, Learning & Development Head, FinEdge Bank",
            image: "../src/assets/images/reviews/rajesh_verma.jpg"
        },
        {
            heading: "Simplified Campus Hiring!",
            description:"Skill Checkr's platform enabled us to conduct large-scale campus hiring seamlessly. The automated grading and AI-driven analysis identified top candidates quickly, improving our time-to-hire by 40%.",
            name: "Sonal Kapoor, Recruitment Lead, ByteWorks Innovations",
            image: "../src/assets/images/reviews/sonal_kapoor.jpg"
        },
        {
            heading: "Reliable for Certification Programs!",
            description:"We used Skill Checkr for our internal certification assessments. The secure proctoring feature and detailed reports ensured credibility and transparency. Highly recommended!",
            name: "Amit Roy, Training Manager, CloudSphere Solutions",
            image: "../src/assets/images/reviews/amit_roy.jpg"
        },
        {
            heading: "Enhanced Training Effectiveness!",
            description:"Skill Checkr has been instrumental in our training initiatives. Employees could practice assessments in a real-exam-like environment, boosting their confidence and performance.",
            name: "Neha Bansal, Head of Talent Development, ZenithCorp",
            image: "../src/assets/images/reviews/neha_bansal.jpg"
        }
    ]
    
    
    return(
        <>
            <div 
                className="flex flex-wrap w-full min-h-[250px] gap-8 justify-center">
                {
                    reviews.map((review,index)=>  <Review review={review} key={index}/> )
                }
                
            </div>
        </>
    )
}