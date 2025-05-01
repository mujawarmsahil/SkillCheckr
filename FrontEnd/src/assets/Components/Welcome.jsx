export default function Welcome({role}) {
    const getContentByRole = (role) => {
        switch (role) {
            case "Teacher":
                return (
                    <p className="text-orange-500 font-ubuntu">
                        Welcome Teacher! Empower students with your knowledge.
                    </p>
                );
            case "Student":
                return (
                    <p className="text-orange-400 font-intel">
                        Welcome Student! Unlock your potential with SkillCheckr.
                    </p>
                );
            case "Admin":
                return (
                    <p className="text-orange-600 font-ubuntu">
                        Welcome Admin! Manage the platform efficiently.
                    </p>
                );
            case "Company":
                return (
                    <p className="text-orange-500 font-intel">
                        Welcome Company! Find the right talent for your needs.
                    </p>
                );
            default:
                return (
                    <p className="text-white font-ubuntu">
                        Welcome to SkillCheckr! Your one-stop platform for skill assessment and recommendations.
                    </p>
                );
        }
    };

    return (
        <div className="welcome-container">
            <h1 className="welcome-title text-orange-600 font-ubuntu">
                Welcome to SkillCheckr
            </h1>
            <div className="welcome-description">{getContentByRole(role)}</div>
        </div>
    );
};
