import "../styles/Topbar.css"
import {
    SquarePen,
    UserSearch,
    Hourglass,
    Search,
    Settings
} from "lucide-react";
import {useEffect, useRef, useState} from "react";
import {useNavigate} from "react-router-dom";
import {mailService} from "../services/MailService.js";

function Topbar({onCompose, onSearch}){
    const [username, setUsername] = useState("");
    const [globalContacts, setGlobalContacts] = useState([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const navigate = useNavigate();

    const dropdownRef = useRef(null);

    //TODO: Fetch username from backend and set it to state
    useEffect(() => {
        setUsername("JohnSmith");

        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleToggleDropdown = async () => {
        const nextState = !showDropdown;
        setShowDropdown(nextState);

        if (nextState){
            try {
                const data = await mailService.fetchGlobalSuggestions();
                setGlobalContacts(data);
            }catch (err) {
                console.error("Error fetching global contacts:", err);
            }
        }
    }

    return (
        <header className={"topbar"}>
            <div className={"topbar-left"}>
                <span className={"app-name"}>Quantum Mail</span>
            </div>

            <div className={"topbar-center"}>
                <button className={"topbar-icon-button"} onClick={() => onCompose?.()}>
                    <SquarePen size={20} />
                </button>

                <div className="user-search-wrapper" ref={dropdownRef}>
                    <button className={`topbar-icon-button ${showDropdown ? "active" : ""}`} onClick={handleToggleDropdown}>
                        <UserSearch size={20} />
                    </button>

                    {showDropdown && (
                        <div className="topbar-user-dropdown">
                            {globalContacts.length === 0 ? (
                                <div className="dropdown-status">No Contacts</div>
                            ) : (
                                <div className="dropdown-scroll-container">
                                    {globalContacts.map((email, index) => (
                                        <div
                                            key={index}
                                            className="dropdown-user-item"
                                            onClick={() => { onCompose?.(email); setShowDropdown(false); }}
                                        >
                                            {email}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <button className={"topbar-icon-button"}>
                    <Hourglass size={20} />
                </button>

                <div className={"search-wrapper"}>
                    <Search size={16} className={"search-icon"} />
                    <input
                        type={"text"}
                        placeholder={"Search"}
                        className={"search-input"}
                        onChange={(e) => onSearch?.(e.target.value)}
                    />
                </div>
            </div>

            <div className={"topbar-right"}>
                <span className={"username"}>{username}</span>

                <button className={"topbar-icon-button"} onClick={() => navigate("/settings")}>
                    <Settings size={20} />
                </button>
            </div>
        </header>
    )
}

export default Topbar;