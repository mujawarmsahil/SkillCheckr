import React from "react";
import BlogPost from "./BlogPost";

export default function Blog() {
    const blogPosts = [
        {
            title: "SkillCheckr Expands Its Reach with New Partnerships",
            author: "Press Release",
            date: "March 25, 2025",
            content:
                "SkillCheckr has announced strategic partnerships with leading universities and enterprises, reinforcing its position as a trusted examination platform. These collaborations will further simplify campus recruitment and online assessments at scale.",
        },
        {
            title: "AI-Powered Assessment Analytics with SkillCheckr",
            author: "Anika Sharma",
            date: "March 12, 2025",
            content:
                "Learn how SkillCheckr’s AI-powered analytics provide deeper insights into candidate performance, helping companies make data-driven hiring decisions. This blog dives into how our algorithms identify top talent efficiently.",
        },
        {
            title: "Ensuring Exam Integrity with Our Advanced Proctoring Solutions",
            author: "Rahul Mehta",
            date: "March 5, 2025",
            content:
                "Maintaining fairness and security during online assessments is crucial. In this post, we explore SkillCheckr’s AI-driven proctoring tools that detect suspicious behavior and ensure exam credibility.",
        },
        // Add more blog posts here
    ];

    return (
        <div 
            className="pt-24 w-[80%] mx-auto pb-12 px-6 font-ubuntu text-black">
            <h1 
                className="text-3xl font-bold">
                <strong>
                    Latest &nbsp;
                    <span 
                        className="p-1 text-white rounded bg-orange-400">
                        Blogs
                    </span>
                </strong>
            </h1>
            {
                blogPosts.map((post, index) => (
                                        <BlogPost
                                            key={index}
                                            title={post.title}
                                            author={post.author}
                                            date={post.date}
                                            content={post.content}
                                        />
                ))
            }
        </div>
    );
}
