export default function Review({review}){
    return(
        <>
            <div className="w-72 min-w-[15rem] mx-4 p-6 bg-white shadow-lg scale-95 transition-all duration-100 rounded-2xl  hover:scale-100 ">
                <img 
                    src={review.image} 
                    alt="userImage" 
                    className="w-16 h-16 object-cover rounded-full mb-4 mx-auto"/>
                <h3
                    className="text-xl font-bold mb-2 text-center">
                    {review.heading}
                </h3>
                <p
                    className="text-gray-600 mb-4 text-justify">
                    <q>{review.description}</q> 
                </p>
                <p
                    className="text-orange-500 italic font-ubuntu text-center">
                    {review.name}
                </p>
            </div>
        </>
    )
}