import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useLocation } from "react-router-dom";

export default function ExamsQueOption() {
  const location = useLocation();
  const examName = location.state?.examName || "Exam Name";
  const question = [
    {
      text: "What is React?",
      opetion1: "React is a framework",
      opetion2: "React is a framework and we build websites",
      opetion3: "React is not a framework",
      opetion4:
        "React is a framework used to build websites, and it's very easy",
      opet: "",
    },
    {
      text: "What language is used with React?",
      opetion1: "Python",
      opetion2: "C++",
      opetion3: "JavaScript",
      opetion4: "Java",
      opet: "",
    },
    {
      text: "Which hook is used to manage state?",
      opetion1: "useEffect",
      opetion2: "useContext",
      opetion3: "useState",
      opetion4: "useRef",
      opet: "",
    },
  ];

  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState(
    Array(question.length).fill(null)
  );

  const handleOptionChange = (optionKey) => {
    const updatedAnswers = [...selectedAnswers];
    updatedAnswers[currentIndex] = optionKey;
    setSelectedAnswers(updatedAnswers);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Selected Answers:", selectedAnswers);
    alert("Answers submitted successfully!");
  };

  return (
    <>
      <div className="min-h-screen w-full bg-slate-50">
        <div className="border-2 border-orange-100 bg-white p-4 flex justify-between items-center">
          <h1 className="text-2xl text-orange-500 font-semibold underline font-ubuntu">
            SkillChecker
          </h1>

          <div className="flex items-center gap-4">
            <label
              htmlFor="examName"
              className="text-2xl text-orange-500 font-semibold"
            >
              Exams Name
            </label>
            <input
              type="text"
              value={examName}
              className="min-w-64 min-h-12"
              readOnly
            />
          </div>
          <Link to="/user/student">
            <button className="border-2 bg-slate-800 shadow-xl w-[8rem] md:w-[10rem] h-[2.5rem] md:h-[3rem] rounded-2xl font-ubuntu text-white hover:bg-orange-500 hover:text-white hover:shadow-2xl transition-all duration-300">
              Dashboard
            </button>
          </Link>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
          <div className="p-4 text-xl font-semibold bg-white">
            {question[currentIndex].text}
          </div>

          <div className="space-y-4">
            {["opetion1", "opetion2", "opetion3", "opetion4"].map(
              (key, index) => (
                <div key={index} className="flex items-center gap-4">
                  <input
                    type="radio"
                    name="option"
                    checked={selectedAnswers[currentIndex] === key}
                    onChange={() => handleOptionChange(key)}
                    className="ml-10"
                  />
                  <input
                    type="text"
                    value={question[currentIndex][key]}
                    readOnly
                    className="w-full border-none bg-slate-50 px-3 py-2 hover:border-orange-200"
                  />
                </div>
              )
            )}
          </div>

          <div className="flex justify-evenly pt-6">
            <button
              type="button"
              onClick={() => {
                if (currentIndex > 0) {
                  setCurrentIndex(currentIndex - 1);
                }
              }}
              disabled={currentIndex === 0}
              className="bg-slate-800 text-white px-6 py-2 rounded-2xl hover:bg-orange-500 disabled:opacity-50"
            >
              Previous
            </button>

            {currentIndex === question.length - 1 ? (
              <button
                type="submit"
                className="bg-green-700 text-white px-6 py-2 rounded-2xl hover:bg-orange-500"
              >
                Submit
              </button>
            ) : (
              <button
                type="button"
                onClick={() => {
                  if (currentIndex < question.length - 1) {
                    setCurrentIndex(currentIndex + 1);
                  }
                }}
                className="bg-slate-800 text-white px-6 py-2 rounded-2xl hover:bg-orange-500"
              >
                Next
              </button>
            )}
          </div>
        </form>
      </div>
    </>
  );
}
