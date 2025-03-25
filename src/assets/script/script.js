document.addEventListener("scroll", () => {
    if(window.scrollY >= 85){
        document.querySelector("#heading").classList.remove("bg-transparent")
        document.querySelector("#heading").classList.add("shadow-xl","bg-white")
    }
    else{
        document.querySelector("#heading").classList.remove("shadow-xl","bg-white")
        document.querySelector("#heading").classList.remove("bg-transparent")
    }
});