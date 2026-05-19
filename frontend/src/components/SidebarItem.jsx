import {FilePen, Mails, SendHorizontal, MessageSquareOff, Star, Trash2,} from "lucide-react";
import "../styles/Sidebar.css";

const icons = {
    Inbox: Mails,
    Drafts: FilePen,
    Sent: SendHorizontal,
    Starred: Star,
    Spam: MessageSquareOff,
    Trash: Trash2,
};

function SidebarItem({item, onClick}) {
    const Icon = icons[item.name] || Mails;

    return (
        <button className={`sidebar-item ${item.active ? "active" : ""}`} onClick={onClick}>
            <Icon size={18}/>

            <span>{item.name}</span>

            {item.unread > 0 && (
                <div className={"badge small"}>{item.unread}</div>
            )}
        </button>
    );
}

export default SidebarItem;