import React from "react";

export default function BlogPost({ title, author, date, content }) {
  return (
    <article className="p-6 bg-white border border-slate-200 rounded-3xl shadow-sm hover:shadow-md transition-shadow space-y-3">
      <div className="flex items-center gap-2 text-xs text-slate-400">
        <span>By {author}</span>
        <span>•</span>
        <span>{date}</span>
      </div>
      <h2 className="text-lg font-bold text-slate-900 leading-snug">{title}</h2>
      <p className="text-xs text-slate-600 leading-relaxed">{content}</p>
    </article>
  );
}
