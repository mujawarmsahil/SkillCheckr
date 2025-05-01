import React, { useState } from "react";
import { Link } from "react-router-dom";

export default function Question() {
  const QuetionsPepar = [
    {
      id: 1,
      question: "What is the capital of Germany?",
      option1: "Berlin",
      option2: "Munich",
      option3: "Hamburg",
      option4: "Frankfurt",
      correctOption: "Berlin",
    },
    {
      id: 2,
      question: "Which planet is known as the Red Planet?",
      option1: "Earth",
      option2: "Mars",
      option3: "Venus",
      option4: "Jupiter",
      correctOption: "Mars",
    },
    {
      id: 3,
      question: "What is the chemical symbol for water?",
      option1: "H2O",
      option2: "O2",
      option3: "CO2",
      option4: "HO",
      correctOption: "H2O",
    },
    {
      id: 4,
      question: "Who wrote 'Romeo and Juliet'?",
      option1: "Charles Dickens",
      option2: "William Shakespeare",
      option3: "Mark Twain",
      option4: "Jane Austen",
      correctOption: "William Shakespeare",
    },
    {
      id: 5,
      question: "What is the largest ocean on Earth?",
      option1: "Atlantic Ocean",
      option2: "Indian Ocean",
      option3: "Arctic Ocean",
      option4: "Pacific Ocean",
      correctOption: "Pacific Ocean",
    },
    {
      id: 6,
      question: "What is the square root of 64?",
      option1: "6",
      option2: "7",
      option3: "8",
      option4: "9",
      correctOption: "8",
    },
    {
      id: 7,
      question: "What is the capital of Japan?",
      option1: "Tokyo",
      option2: "Osaka",
      option3: "Kyoto",
      option4: "Nagoya",
      correctOption: "Tokyo",
    },
    {
      id: 8,
      question: "What is the smallest prime number?",
      option1: "0",
      option2: "1",
      option3: "2",
      option4: "3",
      correctOption: "2",
    },
    {
      id: 9,
      question: "Who painted the Mona Lisa?",
      option1: "Vincent van Gogh",
      option2: "Leonardo da Vinci",
      option3: "Pablo Picasso",
      option4: "Claude Monet",
      correctOption: "Leonardo da Vinci",
    },
    {
      id: 10,
      question: "What is the boiling point of water at sea level?",
      option1: "90°C",
      option2: "100°C",
      option3: "110°C",
      option4: "120°C",
      correctOption: "100°C",
    },
    {
      id: 11,
      question: "What is the largest planet in our solar system?",
      option1: "Earth",
      option2: "Mars",
      option3: "Jupiter",
      option4: "Saturn",
      correctOption: "Jupiter",
    },
    {
      id: 12,
      question: "Who is known as the Father of Computers?",
      option1: "Charles Babbage",
      option2: "Alan Turing",
      option3: "John von Neumann",
      option4: "Ada Lovelace",
      correctOption: "Charles Babbage",
    },
    {
      id: 13,
      question: "What is the speed of light?",
      option1: "300,000 km/s",
      option2: "150,000 km/s",
      option3: "450,000 km/s",
      option4: "600,000 km/s",
      correctOption: "300,000 km/s",
    },
    {
      id: 14,
      question: "What is the capital of France?",
      option1: "Paris",
      option2: "Lyon",
      option3: "Marseille",
      option4: "Nice",
      correctOption: "Paris",
    },
    {
      id: 15,
      question: "What is the smallest unit of matter?",
      option1: "Atom",
      option2: "Molecule",
      option3: "Electron",
      option4: "Proton",
      correctOption: "Atom",
    },
    {
      id: 16,
      question: "Who discovered penicillin?",
      option1: "Alexander Fleming",
      option2: "Marie Curie",
      option3: "Louis Pasteur",
      option4: "Gregor Mendel",
      correctOption: "Alexander Fleming",
    },
    {
      id: 17,
      question: "What is the capital of Italy?",
      option1: "Rome",
      option2: "Milan",
      option3: "Venice",
      option4: "Florence",
      correctOption: "Rome",
    },
    {
      id: 18,
      question: "What is the freezing point of water?",
      option1: "0°C",
      option2: "32°C",
      option3: "100°C",
      option4: "-10°C",
      correctOption: "0°C",
    },
    {
      id: 19,
      question: "Who developed the theory of relativity?",
      option1: "Isaac Newton",
      option2: "Albert Einstein",
      option3: "Galileo Galilei",
      option4: "Nikola Tesla",
      correctOption: "Albert Einstein",
    },
    {
      id: 20,
      question: "What is the tallest mountain in the world?",
      option1: "K2",
      option2: "Mount Everest",
    },
  ];

  return (
    <>
      <div className=" w-full h-svh flex justify-center bg-white ">
        <div className=" flex-col justify-center mt-10 w-full overflow-y-auto bg-white relative pb-10">
          <div className=" w-full  flex justify-around items-center bg-white sticky top-0 z-10 h-20">
            <h1 className="text-xl md:text-2xl font-semibold text-center py-5 underline text-orange-500  hover:text-black font-ubuntu">
              Show Exams Component
            </h1>
            <Link to="/user/teacher">
              <button className="border-2 bg-slate-800 shadow-xl w-[8rem] md:w-[10rem] h-[2.5rem] md:h-[3rem] rounded-2xl font-ubuntu text-white hover:bg-orange-500 hover:text-white hover:shadow-2xl transition-all duration-300">
                Go to Dashboard
              </button>
            </Link>
          </div>

          <div className="w-full overflow-x-auto mt-6 max-h-[70vh] overflow-y-auto">
            <table className="min-w-[700px] md:min-w-full text-center bg-white shadow-lg rounded-lg">
              <thead className="bg-gray-200 sticky top-0 z-10 text-xs md:text-sm">
                <tr>
                  <th className="border px-2 md:px-4 py-2">QId</th>
                  <th className="border px-2 md:px-4 py-2">Question</th>
                  <th className="border px-2 md:px-4 py-2">Op 1</th>
                  <th className="border px-2 md:px-4 py-2">Op 2</th>
                  <th className="border px-2 md:px-4 py-2">Op 3</th>
                  <th className="border px-2 md:px-4 py-2">Op 4</th>
                </tr>
              </thead>
              <tbody>
                {QuetionsPepar.map((exam) => (
                  <tr key={exam.id} className="text-xs md:text-sm">
                    <td className="border px-2 md:px-4 py-2">{exam.id}</td>
                    <td className="border px-2 md:px-4 py-2">
                      {exam.question}
                    </td>
                    <td
                      className={`border px-2 md:px-4 py-2 ${
                        exam.option1 === exam.correctOption
                          ? "text-green-500 font-medium"
                          : "text-black"
                      }`}
                    >
                      {exam.option1}
                    </td>
                    <td
                      className={`border px-2 md:px-4 py-2 ${
                        exam.option2 === exam.correctOption
                          ? "text-green-500 font-medium"
                          : "text-black"
                      }`}
                    >
                      {exam.option2}
                    </td>
                    <td
                      className={`border px-2 md:px-4 py-2 ${
                        exam.option3 === exam.correctOption
                          ? "text-green-500 font-medium"
                          : "text-black"
                      }`}
                    >
                      {exam.option3}
                    </td>
                    <td
                      className={`border px-2 md:px-4 py-2 ${
                        exam.option4 === exam.correctOption
                          ? "text-green-500 font-medium"
                          : "text-black"
                      }`}
                    >
                      {exam.option4}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
}
