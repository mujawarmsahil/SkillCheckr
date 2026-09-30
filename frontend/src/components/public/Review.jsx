import React from "react";

export default function Review({ review }) {
  return (
    <div className="w-full sm:w-80 bg-white p-6 rounded-3xl border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between space-y-4">
      <div className="space-y-2">
        <div className="flex text-amber-400 text-sm">★★★★★</div>
        <h3 className="font-bold text-slate-800 text-sm">{review.heading}</h3>
        <p className="text-xs text-slate-500 leading-relaxed italic">
          "{review.description}"
        </p>
      </div>
      <div className="pt-3 border-t border-slate-100 flex items-center gap-3">
        <div className="w-9 h-9 rounded-full bg-orange-100 text-orange-600 font-bold flex items-center justify-center text-xs">
          {review.name.charAt(0)}
        </div>
        <div>
          <h4 className="text-xs font-bold text-slate-800">{review.name.split(",")[0]}</h4>
          <p className="text-[10px] text-slate-400">{review.name.split(",").slice(1).join(",") || "Educator"}</p>
        </div>
      </div>
    </div>
  );
}
