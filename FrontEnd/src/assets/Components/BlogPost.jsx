export default function BlogPost({ title, author, date, content }) {
    return (
        <div 
            className="py-6 px-6 font-ubuntu text-black border-b border-gray-300">
            <h2 
                className="text-2xl font-bold text-orange-400">
                {title}  
            </h2>
            <p 
                className="text-sm text-gray-600 mt-1">
                By 
                <span 
                    className="font-semibold">
                    {author}
                </span> 
                on {date}
            </p>
            <p 
                className="mt-4 text-lg font-intel">
                {content}    
            </p>
        </div>
    );
}