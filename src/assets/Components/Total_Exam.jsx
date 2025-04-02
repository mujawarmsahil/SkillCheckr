import React from "react";
import { PieChart, Pie, Tooltip, Legend, Cell } from "recharts";
// import { useState } from "react";
export default function Tota_Exam() {


  const data = [
    { name: "All Exames", value: 3540 },
    { name: "Current Exams", value: 3500 },
    { name: "Upcoming exams ", value: 7540 },
  ];

  const [,exams,setExames] = React.useState([
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },
    { id: 0, name: "Ganesh Rathod", date: "2023-10-01", time: "10:00 AM", type: "MCQ" },]);
  
// const [effectData, setEffectData] = useState(null);

// const handlePieClick = (entry) => {
//   if (entry.name === "Teachers") {
//     setEffectData("teachers"); // Show teacher data
//   } else if (entry.name === "Students") {
//     setEffectData("students"); // Show student data
//   }
// };

  const COLORS = ["blue", "gray"];
  return (
    <div className="w-full min-h-screen border-2 bg-slate-50 py-10 border-red-400 flex flex-col items-center">
          <h1 className="text-2xl font-bold p-3  font-ubuntu text-slate-800 underline">
            📑 Total Exams Component
            </h1>
            {/* <div className="w-full flex flex-col lg:flex-row items-center justify-center mt-10 gap-10"> */}
      <div className=" w-full flex border-2 border-blue-400 flex-col items-center justify-center mt-10  gap-10">

 
<div className=" border-2 border-green-400 w-full justify-center items-center flex  lg:w-1/3  ">

 <PieChart width={500} height={400}>
            <Pie
              // onClick={handlePieClick}
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={90}
              outerRadius={160}
              fill="#8884d8"
              dataKey="value"
              label
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
</div>
           

           <div className=" border-2 border-red-400 w-full flecx items-center justify-center">

         <h1 className="flex justify-center text-2xl font-semibold text-slate-800 underline"> 
          Hello i am mangesh Wagh 
          </h1>
<div className="overflow-y-auto max-h-[300px] w-full">
  <table className="border-2 w-full text-center bg-white shadow-lg rounded-lg mt-6">
                   <thead className="bg-gray-200">
                     <tr>
                       <th className="border px-4 py-2">Sr</th>
                       <th className="border px-4 py-2">Exam Name</th>
                       <th className="border px-4 py-2">Exam Date</th>
                       <th className="border px-4 py-2">Exam Time</th>
                       <th className="border px-4 py-2">Exam Duration</th>
                       <th className="border px-4 py-2">Exam Type</th>
                     </tr>
                   </thead>
                   <tbody>
                     {/* Map through your data here */}

                     {students.map((exams) => (
                      <tr key={exams.id}>
                        <td className="border-2 px-4 py-2">{exams.id}</td>
                        <td className="border-2 px-4 py-2">{exams.name}</td>
                        <td className="border-2 px-4 py-2">{exame.date}</td>
                        <td className="border-2 px-4 py-2">{exame.time}</td>
                        <td className="border-2 px-4 py-2">{exame.type}</td>
      
                      </tr>
                    ))}

                   </tbody>
                 </table>
  <h2>
    mangesh wagh tigers 
  </h2>


</div>
           </div>

      </div>
    </div>
  );
}