import React from "react";
import Review from "./Review";

export default function Card() {
  const reviews = [
    {
      heading: "Fast, accurate assessments.",
      description:
        "SkillCheckr cut our grading workload and gave us reliable results per question. It saves hours of manual evaluation.",
      name: "Dr. Anita Sharma, Academic Dean, TechNova Institute",
    },
    {
      heading: "Scores match skill, not luck.",
      description:
        "Supporting both MCQ and written answers lets our teachers test conceptual understanding as well as speed.",
      name: "Rajesh Verma, Department Head, FinEdge Academy",
    },
    {
      heading: "Students can focus on the exam.",
      description:
        "The distraction-free exam view and instant result breakdowns keep testing transparent for every student.",
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
