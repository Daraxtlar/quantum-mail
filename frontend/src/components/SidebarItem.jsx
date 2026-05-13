import {
    Mail,
    FileText,
    Send,
    Star,
    ShieldAlert,
    Trash2,
} from "lucide-react";
import "../styles/Sidebar.css";

const icons = {
    Inbox: Mail,
    Drafts: FileText,
    Sent: Send,
    Starred: Star,
    Spam: ShieldAlert,
    Trash: Trash2,
};

function SidebarItem({item, onClick}) {
        const Icon = icons[item.name] || Mail;

    return (
        <button className={`sidebar-item ${item.active ? "active" : ""}`} onClick={onClick}>
            <Icon size={18} />

            <span>{item.name}</span>

            {item.unread > 0 && (
               <div className={"badge small"}>{item.unread}</div>
            )}
        </button>
    );
}

export default SidebarItem;