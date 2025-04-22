import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
  RouterProvider,
} from "react-router-dom";
import "./index.css";
import App from "./App.jsx";
import Home from "./assets/Components/Home.jsx";
import About from "./assets/Components/About.jsx";
import Blog from "./assets/Components/Blog.jsx";
import Contact from "./assets/Components/Contact.jsx";
import Authentication from "./assets/Components/Authentication.jsx";
import Dashboard from "./assets/Components/Dashboard.jsx";
import Question from "./assets/Components/Question.jsx";
import Create_Exams from "./assets/Components/Create_Exams.jsx";
import ExamsQueOption from "./assets/Components/ExamsQueOpetion.jsx";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<App />}>
        <Route path="" element={<Home />} />
        <Route path="about" element={<About />} />
        <Route path="blog" element={<Blog />} />
        <Route path="contact" element={<Contact />} />
      </Route>
      <Route path="/authentication" element={<Authentication />} />
      <Route path="/user/:role" element={<Dashboard />} />
      <Route path="/question" element={<Question />} />
      <Route path="/createExam" element={<Create_Exams />} />
      <Route path="/studentExams" element={<ExamsQueOption />} />
    </>
  )
);

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>
);
