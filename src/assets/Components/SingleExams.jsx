import React from "react";
import SingleExCard from "./SingleExCard";

export default function SingleExams() {
  const singleExameCard = [
    {
      examName: "1)BBA DEC",
      examsDetais:
        "Skill Checkr streamlined our recruitment process by providing efficient and accurate assessments Skill Checkr streamlined our recruitment process by providing efficient and accurate assessments.",
      button: "Show Quetions",
    },
    {
      examName: "2)MSC Jan",
      examsDetais:
        "Skill Checkr helped us identify top talent quickly and effectively for our organization. macds s cSkill Checkr helped us identify top talent quickly and effectively for our organization. macds s c  ",
      button: "Show Quetions",
    },
    {
      examName: "3)MCA FEb",
      examsDetais:
        "Skill Checkr's platform is user-friendly and delivers precise results for better hiring decisions Skill Checkr's platform is user-friendly and delivers precise results for better hiring decisions better hiring decisions Skill Checkr's platform is user-friendly and delivers precise results for better hiring decisions.",
      button: "Show Quetions",
    },
    
  
  ];
  return (
    <>
      <div className="  w-full  min-h-screen bg-slate-50 flex  flex-col itmes-center ">
        <h1 className="text-2xl text-slate-800 font-semibold h-20 w-full mt-10 font-ubuntu text-center py-4 underline">
          📝 Single Exam Component
        </h1>

        {/* <div className="w-full flex justify-evenly p-10   mt-5  "> */}
        <div className="w-full flex flex-wrap justify-center gap-6 px-4 py-6">
          {singleExameCard.map((singleExameCard, index) => (
            <SingleExCard singleExameCard={singleExameCard} key={index} />
          ))}
        </div>
      </div>
    </>
  );
}
