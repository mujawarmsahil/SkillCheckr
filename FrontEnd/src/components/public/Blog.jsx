import React from "react";
import BlogPost from "./BlogPost";

export default function Blog() {
  const blogPosts = [
    {
      title: "The Evolution of MCQ vs Descriptive Academic Testing in Higher Ed",
      author: "Dr. Anika Sharma",
      date: "March 20, 2026",
      content:
        "While multiple-choice examinations provide unmatched grading efficiency and objective scoring, descriptive assessments test critical synthesis, problem articulation, and conceptual depth. Discover how modern assessment platforms are combining both paradigms.",
    },
    {
      title: "Eliminating Assessment Bias: Data-Driven Performance Analytics",
      author: "Rahul Mehta",
      date: "March 12, 2026",
      content:
        "Real-time test diagnostics allow educators to identify question difficulty outliers, curriculum comprehension gaps, and learning progression trends across diverse student cohorts.",
    },
    {
      title: "Ensuring Integrity and Reliability in Digital Examination Workflows",
      author: "SkillCheckr Research Team",
      date: "March 5, 2026",
      content:
        "From autosaved local drafts to tamper-resistant session states and administrative verification queues, explore the core technical safeguards that make academic testing resilient.",
    },
  ];

  return (
    <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-8">
      <div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">
          Latest <span className="text-orange-500">Articles & Insights</span>
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Explore research and best practices on modern educational assessments
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {blogPosts.map((post, index) => (
          <BlogPost
            key={index}
            title={post.title}
            author={post.author}
            date={post.date}
            content={post.content}
          />
        ))}
      </div>
    </section>
  );
}
