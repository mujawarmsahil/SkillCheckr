import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
// import ExamsQueOption from "./ExamsQueOpetion";

export default function Exam() {
  const [exams, setExams] = useState([]);

  useEffect(() => {
    // Simulate fetching data from an API
    const fetchExams = async () => {
      const data = [
        {
          exam_id: 1,
          exam_name: "Mathematics Midterm",
          exam_date: "2023-12-01T10:00:00",
          subject_name: "Mathematics",
        },
        {
          exam_id: 2,
          exam_name: "Physics Final",
          exam_date: "2023-12-05T14:00:00",
          subject_name: "Physics",
        },
        {
          exam_id: 3,
          exam_name: "Chemistry Quiz",
          exam_date: new Date().toISOString(), // Current exam
          subject_name: "Chemistry",
        },
      ];
      setExams(data);
    };

    fetchExams();
  }, []);

  const formatDate = (dateString) => {
    const options = {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    };
    return new Date(dateString).toLocaleDateString(undefined, options);
  };

  return (
    <div>
      <h1>Exams</h1>
      {exams.length > 0 ? (
        <ul className="flex flex-wrap gap-[30px] my-2">
          {exams.map((exam) => (
            <li
              key={exam.exam_id}
              className="w-[300px] h-[150px] flex gap-2 flex-col bg-orange-50 p-2 rounded-sm shadow-[0_0_5px_1px_black]"
            >
              <strong>{exam.exam_name}</strong> - {exam.subject_name} <br />
              <em>{formatDate(exam.exam_date)}</em>
              <Link to="/studentExams"
               state={{ examName: exam.exam_name }}
              
              >
                <button
                  type="button"
                  className="bg-orange-400 w-[100px] mx-auto hover:bg-orange-500 text-white p-1  rounded"
                >
                  Start exam
                </button>
              </Link>
            </li>
          ))}
        </ul>
      ) : (
        <p>No upcoming exams. </p>
      )}
    </div>
  );
}
