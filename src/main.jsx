import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter,createRoutesFromElements,Route ,RouterProvider } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import Home from './assets/Components/Home.jsx'
import About from './assets/Components/About.jsx'
import Blog from './assets/Components/Blog.jsx'
import Contact from './assets/Components/Contact.jsx'
import Authentication from './assets/Components/Authentication.jsx'
import Dashboard from './assets/Components/Dashboard.jsx'
import { Provider } from 'react-redux'
import store from './assets/Store/Store.js'

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<App/>}>
        <Route path="" element={<Home/>}/>
        <Route path="about" element={<About/>}/>
        <Route path="blog" element={<Blog/>}/>
        <Route path="contact" element={<Contact/>}/>
      </Route>
      <Route path="/authentication" element={<Authentication/>}/>
      <Route path="/user/:role" element={<Dashboard/>}/>
    </>
  )
)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <RouterProvider
        router={router}/>
    </Provider>
  </StrictMode>,
)