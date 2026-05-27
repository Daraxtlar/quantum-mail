import "../styles/Topbar.css"
import {
    SquarePen,
    UserSearch,
    Hourglass,
    Search,
    Settings
} from "lucide-react";
import {useEffect, useState} from "react";

function Topbar({onCompose}){
    const [username, setUsername] = useState("")

    //TODO: Fetch username from backend and set it to state
    useEffect(() => {
        setUsername("JohnSmith");
    }, []);

    return (
        <header className={"topbar"}>
            <div className={"topbar-left"}>
                <span className={"app-name"}>Quantum Mail</span>
            </div>

            <div className={"topbar-center"}>
                <button className={"topbar-icon-button"} onClick={onCompose}>
                    <SquarePen size={20} />
                </button>

                <button className={"topbar-icon-button"}>
                    <UserSearch size={20} />
                </button>

                <button className={"topbar-icon-button"}>
                    <Hourglass size={20} />
                </button>

                <div className={"search-wrapper"}>
                    <Search size={16} className={"search-icon"} />
                    <input type={"text"} placeholder={"Search"} className={"search-input"} />
                </div>
            </div>

            <div className={"topbar-right"}>
                <span className={"username"}>{username}</span>

                <button className={"topbar-icon-button"}>
                    <Settings size={20} />
                </button>
            </div>
        </header>
    )
}

export default Topbar;