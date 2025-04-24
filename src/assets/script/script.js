// document.addEventListener("scroll", () => {
//     if(window.scrollY >= 50){
//         document.querySelector("#heading").classList.remove("bg-transparent")
//         document.querySelector("#heading").classList.add("shadow-xl","bg-white")
//     }
//     else{
//         document.querySelector("#heading").classList.remove("shadow-xl","bg-white")
//         document.querySelector("#heading").classList.remove("bg-transparent")
//     }
// });




document.addEventListener("DOMContentLoaded", () => {
    const heading = document.querySelector("#heading");
  
    if (!heading) return; // agar heading nahi mila toh kuch bhi mat karo
  
    document.addEventListener("scroll", () => {
      if (window.scrollY >= 50) {
        heading.classList.remove("bg-transparent");
        heading.classList.add("shadow-xl", "bg-white");
      } else {
        heading.classList.remove("shadow-xl", "bg-white");
        heading.classList.add("bg-transparent");
      }
    });
  });
  