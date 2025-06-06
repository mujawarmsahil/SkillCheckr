import axios from "axios";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";

export default function Create_Exams() {
  const [questionAns, setQuestionAns] = useState({
    question: "",
    option1: "",
    option2: "",
    option3: "",
    option4: "",
    correctOption: "",
  });
  const [examName, setExamName] = useState("");
  const [allQuestions, setAllQuestions] = useState([]);

  useEffect(() => {
    const name = localStorage.getItem("examName");
    if (name) {
      setExamName(name);
    }
  }, []);

  const handleChange = (e) => {
    setQuestionAns({ ...questionAns, [e.target.name]: e.target.value });
  };

  const handleCorrectOption = (value) => {
    setQuestionAns({ ...questionAns, correctOption: value });
  };

  const handleAddQuestion = () => {
    if (
      questionAns.question &&
      questionAns.option1 &&
      questionAns.option2 &&
      questionAns.option3 &&
      questionAns.option4 &&
      questionAns.correctOption
    ) {
      setAllQuestions([...allQuestions, questionAns]);
      setQuestionAns({
        question: "",
        option1: "",
        option2: "",
        option3: "",
        option4: "",
        correctOption: "",
      });
    } else {
      alert("Please fill in all fields and select the correct option.");
    }
  };

  const handleSubmit = async () => {
    if (allQuestions.length === 0) {
      alert("Please add at least one question before submitting.");
      return;
    }
    const subject_Id = localStorage.getItem("subject_id");
    console.log("Subject ID:", subject_Id);

    const payload = allQuestions.map((q) => ({
      subjectId: subject_Id,
      question: q.question,
      option1: q.option1,
      option2: q.option2,
      option3: q.option3,
      option4: q.option4,
      correctOption: q.correctOption,
    }));
    console.log(
      "Payload to send  We the Send the Data Backend We Check the First now................... :",
      payload
    );
    try {
      const response = await axios.post(
        "http://localhost:8080/api/create/addQues",
        payload
      );
      const subject_id = response.data.subjectId;
      if (subject_id) {
        localStorage.setItem("subject_id", subject_id);
        alert("Exams Created Successfully - Subject ID: " + subject_id);
      }
      console.log("Backend Response :", response.data);
      alert("All Questions Submitted!");
      setAllQuestions([]);
    } catch (error) {
      console.error("Error submitting questions:", error);
      alert("Failed to submit questions. Please try again.");
    }
    console.log("Submitted Questions:", allQuestions);
    // alert("All Questions Submitted!");
  };

  return (
    <div className="w-full min-h-screen bg-white">
      {/* Fixed Header */}
      <div className="fixed top-0 left-0 w-full bg-white z-50 shadow-md py-4 px-6 flex justify-between items-center border-b border-blue-950">
        <h1 className="text-2xl font-bold text-orange-500 underline font-ubuntu">
          Create Exams
        </h1>

        <div className="flex items-center gap-2">
          <label className="text-2xl font-bold">Exam Name:</label>
          <input
            type="text"
            value={examName}
            readOnly
            className="border p-2 text-center text-xl font-semibold font-ubuntu rounded"
          />
        </div>

        <Link to="/user/teacher">
          <button className="bg-slate-800 text-white px-6 py-2 rounded-xl hover:bg-orange-500 transition-all">
            Go to Dashboard
          </button>
        </Link>
      </div>

      {/* Main Content */}
      <div className="pt-[120px] w-full flex justify-center">
        <div className="w-full md:w-[80%] px-4">
          <form className="space-y-6">
            {" "}
            {/* this is The Form We Want The Using the Submit  */}
            <div>
              <label className="font-medium text-2xl text-orange-500 underline underline-offset-4 ">
                Question
              </label>
              <input
                type="text"
                name="question"
                value={questionAns.question}
                onChange={handleChange}
                className="w-full p-3 border rounded mt-1"
              />
            </div>
            {[1, 2, 3, 4].map((num) => (
              <div key={num} className="flex items-center gap-4 ">
                {/*  OPetion Div  */}
                <input
                  type="text"
                  name={`option${num}`}
                  value={questionAns[`option${num}`]}
                  onChange={handleChange}
                  placeholder={`Option ${num}`}
                  className="w-full p-2 border rounded"
                />
                <input
                  type="checkbox"
                  checked={
                    questionAns.correctOption === questionAns[`option${num}`]
                  }
                  onChange={() =>
                    handleCorrectOption(questionAns[`option${num}`])
                  }
                />
                <span className="text-sm">Correct</span>
              </div>
            ))}
          </form>

          <div className="flex justify-evenly mt-8">
            <button
              onClick={handleAddQuestion}
              className="bg-slate-800 w-[20%] h-16 text-white px-6 py-2 rounded-xl hover:bg-orange-500 transition-all"
            >
              Add
            </button>
            <button
              onClick={handleSubmit}
              className="bg-blue-500 w-[20%] h-16 text-white px-6 py-2 rounded hover:bg-blue-600"
            >
              Submit
            </button>
          </div>

          <div className="mt-10 border-2 h-[300px] overflow-y-auto p-2 rounded">
            <h2 className=" font-bold underline mb-3 text-lg text-orange-500">
              Added Questions:
            </h2>
            {allQuestions.map((q, index) => (
              <div key={index} className="border p-3 mb-2">
                <p>
                  <strong>Q{index + 1}:</strong> {q.question}
                </p>
                {[1, 2, 3, 4].map((num) => {
                  const opt = q[`option${num}`];
                  return (
                    <p key={num}>
                      {opt}{" "}
                      {opt === q.correctOption && (
                        <span className="text-green-500 font-semibold">
                          (Correct)
                        </span>
                      )}
                    </p>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
