import React from "react";
import { Link } from "react-router-dom";

export default function SingleExCard({ singleExameCard }) {
  return (
    <div className="w-80 min-w-[18rem] mx-5 p-6 bg-white shadow-lg scale-100 transition-all duration-200 rounded-2xl hover:scale-105">
      <h3 className="text-xl font-bold mb-3 text-center">
        {singleExameCard.examName}
      </h3>
      <p className="text-gray-700 mb-4 text-justify">
        <q>{singleExameCard.examsDetais}</q>
      </p>
      <Link to="/question">
        <button className="w-40 bg-orange-500 h-10  mt-5 ml-10  hover:bg-orange-600 text-white rounded-lg">
          {singleExameCard.button}
        </button>
      </Link>
    </div>
  );
}
