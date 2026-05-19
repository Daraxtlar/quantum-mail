import Topbar from "../components/Topbar.jsx";
import Sidebar from "../components/Sidebar.jsx";
import MailList from "../components/MailList.jsx";
import "../styles/Inbox.css"

function Inbox() {
    return (
        <div className={"inbox"}>
            <Topbar />
            <div className={"layout"}>
                <Sidebar />
                <MailList />
            </div>

        </div>
    )
}

export default Inbox;