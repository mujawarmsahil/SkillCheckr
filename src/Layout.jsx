import {Outlet} from "react-router-dom"
import Navbar from "./assets/Components/Navbar"
import Footer from "./assets/Components/Footer"

export default function Layout(){
    return (
        <>
                <Navbar/>
                <Outlet/>
                <Footer/>
        </>
    )
}
