import "../styles/Mail-list.css"


function MailItem({ mail, isSelected, onToggle, onClick}) {

    const formatDate = (date) => {
        if (!date) return "";
        const d = new Date(date);
        const now = new Date();
        const diffMs = now - d;
        const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        if (days === 0) return "Today";
        if (days === 1) return "Yesterday";
        if (days < 7) return `${days} ago`;
        return d.toLocaleDateString();
    }


    return(
        <div className={`mail-item ${mail.active ? "active" : ""}`} onClick={onClick}>
            <div className={"mail-status"} style={{backgroundColor: mail.color || "transparent"}}></div>

            <div className={"mail-checkbox"} onClick={(e) => e.stopPropagation()}>
                <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={onToggle}
                />
            </div>

            <div className={"mail-sender"}>
                {mail.sender}
            </div>

            <div className={"mail-subject"}>
                {mail.subject}
            </div>

            <div className={"mail-preview"}>
                {mail.preview}
            </div>

            <div className={"mail-date"}>
                {formatDate(mail.date)}
            </div>
        </div>
    )
}

export default MailItem;