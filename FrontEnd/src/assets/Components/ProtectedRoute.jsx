import React, { use } from "react";
import { Navigate } from "react-router-dom";
const ProtectedRoute = ({ children }) => {
//   const i = (username = localStorage.getItem("username"));
const username = localStorage.getItem("username");
  return username ? children:<Navigate to="/authentication" replace/>;
};
export default ProtectedRoute;
