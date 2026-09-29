import React from "react";
import History from "./History";
import Values from "./Values";

export default function About() {
  return (
    <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
      <History />
      <div className="border-t border-slate-200"></div>
      <Values />
    </section>
  );
}
