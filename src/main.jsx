import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter,createRoutesFromElements,Route ,RouterProvider } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import Home from './assets/Components/Home.jsx'
import About from './assets/Components/About.jsx'
import Service from './assets/Components/Service.jsx'
import Contact from './assets/Components/Contact.jsx'
import Authentication from './assets/Components/Authentication.jsx'

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
    <Route path="/" element={<App/>}>
      <Route path="" element={<Home/>}/>
      <Route path="about" element={<About/>}/>
      <Route path="service" element={<Service/>}/>
      <Route path="contact" element={<Contact/>}/>
    </Route>
    <Route path="/authentication" element={<Authentication/>}/>
    </>
  )
)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <RouterProvider
      router={router}/>
  </StrictMode>,
)
