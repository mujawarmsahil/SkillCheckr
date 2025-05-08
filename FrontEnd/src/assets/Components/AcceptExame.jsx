import React from "react";
import { useState, useEffect } from "react";
import axios from "axios";
export default function AcceptExame() {
  const [actionExam, setExamsStatus] = useState([]);

  useEffect(() => {
    axios
      .get("http://localhost:8080/api/Exams/viewAllExams")
      .then(({ data }) => {
        setExamsStatus(data);

        console.log("Exams data:", data);
      })

      .catch((error) => {
        console.error("Error fetching exams:", error);
      });
  }, []);

  const updateStatus = (id, status) => {
    setExamsStatus(
      actionExam.map((exams) =>
        exams.id === id ? { ...exams, Status: status } : exams
      )
    );
  };
  const deleteExams = async (id) => {
    try {
      const response = await axios.delete(
        `http://localhost:8080/api/Exams/deleteExamById/${id}`
      );
      if (response.status == 200) {
        alert("Exam Delete succesfully");
        const updateExam = actionExam.filter((exam) => exam.exam_id != id);
        setExamsStatus(updateExam);
      } else {
        alert("Failed to delete Exam.");
      }
    } catch (error) {
      console.error(" Error deleting Exam", error);
      alert(" Somethimg went wrong while deleting the exams");
    }
    // setExamsStatus(actionExam.filter((exams) => exams.id !== id));
  };

  return (
    <>
      <div className="w-full min-h-screen border-2 bg-slate-50 ">
        {/* Thise is a main div measn all */}
        <div className=" font-bold  p-5 flex underline justify-center ">
          {" "}
          <h1 className="text-late-800 text-2xl ">
            <div>✅ Accept Exam Component</div>
          </h1>
        </div>

        <div className="overflow-y-auto max-h-[500px] flex justify-center  w-full">
          <table className="border-2 w-[90%] text-center bg-white shadow-lg rounded-lg mt-6">
            <thead className="bg-gray-200">
              <tr>
                <th className="border px-4 py-2">ID</th>
                <th className="border px-4 py-2">Exam Name</th>
                <th className="border px-4 py-2">Exams Date</th>

                <th className="border px-4 py-2">StartTIme </th>
                <th className="border px-4 py-2">EndTime</th>
                <th className="border px-4 py-2">Status</th>

                <th className="border px-4 py-2">Action</th>
              </tr>
            </thead>
            <tbody>
              {actionExam.map(
                (exam) => (
                  console.log("Exam data:", exam),
                  (
                    <tr key={exam.exam_id}>
                      <td className="border-2 px-4 py-2">{exam.exame_id}</td>
                      <td className="border-2 px-4 py-2">{exam.exam_name}</td>
                      <td className="border-2 px-4 py-2">{exam.date}</td>
                      <td className="border-2 px-4 py-2">{exam.start_time}</td>
                      <td className="border-2 px-4 py-2">{exam.end_time}</td>
                      <td className="border-2 px-4 py-2">{exam.status}</td>
                      <td className="border-2 px-4 py-2">
                        <button
                          className="bg-slate-800 shadow-xl mr-10 m-1 hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded"
                          onClick={() => updateStatus(exam.exame_id, "Accepted")}
                        >
                          Accept Exam
                        </button>
                        <button
                          className="bg-slate-800 shadow-xl hover:bg-sky-600 hover:text-white text-white px-3 py-3 rounded"
                          onClick={() => deleteExams(exam.exame_id)}
                        >
                          Delete Exam
                        </button>
                      </td>
                    </tr>
                  )
                )
              )}
            </tbody>
          </table>
        </div>
      </div>
      {/* </div> */}
    </>
  );
}
