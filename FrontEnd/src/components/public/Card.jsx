import React from "react";
import Review from "./Review";

export default function Card() {
  const reviews = [
    {
      heading: "Efficient Assessment Solution!",
      description:
        "SkillCheckr streamlined our examination process by providing reliable assessments and detailed performance analytics. It saved hours of manual evaluation.",
      name: "Dr. Anita Sharma, Academic Dean, TechNova Institute",
    },
    {
      heading: "Accurate Skill Evaluation!",
      description:
        "The dual support for both MCQ and Descriptive questions allows our faculty to test conceptual understanding alongside speed and accuracy.",
      name: "Rajesh Verma, Department Head, FinEdge Academy",
    },
    {
      heading: "Seamless Student Experience!",
      description:
        "The distraction-free exam interface and instant scorecard breakdowns make testing transparent, fair, and engaging for all candidates.",
      name: "Sonal Kapoor, Lead Educator, ByteWorks",
    },
  ];

  return (
    <div className="flex flex-wrap gap-6 justify-center">
      {reviews.map((review, index) => (
        <Review review={review} key={index} />
      ))}
    </div>
  );
}
