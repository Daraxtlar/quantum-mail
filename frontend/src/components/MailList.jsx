import "../styles/Mail-list.css";
import MailItem from "./MailItem.jsx";
import {useEffect, useState} from "react";
import {Archive, Funnel, MoveRight, Trash2, Star} from "lucide-react"

//Przykładowe maile
const mails = [
    {
        id: 1,
        sender: "Laura Shea",
        subject: "Closing the deal",
        preview:
            "This is a snippet of text, it'll show the preview of content inside...",
        date: "Yesterday",
        active: true,
        color: "#7CFF5B",
    },
    {
        id: 2,
        sender: "Jonathan, Brett",
        subject: "Start screen & next step",
        preview:
            "This is a snippet of text, it'll show the preview of content...",
        date: "Yesterday",
    },
    {
        id: 3,
        sender: "Conrad Irvin",
        subject: "RE: hey",
        preview:
            "This is a snippet of text, it'll show the preview of content inside...",
        date: "Yesterday",
    },
    {
        id: 4,
        sender: "Vivek",
        subject: "Closing our loop",
        preview:
            "This is a snippet of text, it'll show the preview of content inside...",
        date: "Yesterday",
    },
    {
        id: 5,
        sender: "Rahul Vohra",
        subject: "Important action needed",
        preview:
            "This is a snippet of text, it'll show the preview of content...",
        date: "21/1/2025",
        color: "#FF2D2D",
    },
];

function MailList() {
    const [path, setPath] = useState("")
    const [selectedMails, setSelectedMails] = useState([]);

    useEffect(() => {
        setPath("/JohnSmith@gmail.com/Inbox")
    }, []);

    const allSelected = mails.length > 0 && selectedMails.length === mails.length;

    const handleSelectAll = () => {
        if (allSelected){
            setSelectedMails([]);
        } else{
            setSelectedMails(mails.map(mail => mail.id));
        }
    };

    const handleToggleMail = (mailId) => {
        setSelectedMails(prev => {
            if(prev.includes(mailId)){
                return prev.filter(id => id !== mailId);
            } else{
                return [...prev, mailId];
            }
        });
    };

    const handleDelete = () => {
        //TODO: Implement delete functionality, send selectedMails to backend for deletion
    };

    const handleArchive = () => {
        //TODO: Implement archive functionality, send selectedMails to backend for archiving
    };

    const handleStar = () => {
        //TODO: Implement star functionality, send selectedMails to backend for starring
    };


    return(
        <section className={"mail-section"}>
            <div className={"mail-toolbar"}>
                <div className={"toolbar-left"}>
                    <input type={"checkbox"} checked={allSelected} onChange={handleSelectAll}/>

                    {selectedMails.length > 0 && (
                        <div className={"action-buttons"}>
                            <button className={"action-btn"} onClick={handleArchive} title={"Archive"}>
                                <Archive size={16} />
                            </button>

                            <button className={"action-btn"} onClick={handleDelete} title={"Delete"}>
                                <Trash2 size={16} />
                            </button>

                            <button className={"action-btn"} onClick={handleStar} title={"Star"}>
                                <Star size={16} />
                            </button>

                            <button className={"action-btn"} title={"Move"}>
                                <MoveRight size={16} />
                            </button>
                            <span className={"selected-count"}>{selectedMails.length} selected</span>
                        </div>
                    )}
                </div>

                <div className={"toolbar-right"}>
                    <span className={"path-text"}>{path}</span>


                    <button className={"filter-btn"}>
                        <Funnel size={16} />
                    </button>
                    <span>everything</span>
                </div>
            </div>

            <div className={"mail-list"}>
                {
                    mails.map(mail => (
                        <MailItem
                            key={mail.id}
                            mail={mail}
                            isSelected={selectedMails.includes(mail.id)}
                            onToggle={() => handleToggleMail(mail.id)}
                        />
                    ))
                }
            </div>
        </section>
    )
}

export default MailList;
