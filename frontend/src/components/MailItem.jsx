import "../styles/Mail-list.css"


function MailItem({ mail, isSelected, onToggle}) {
    return(
        <div className={`mail-item ${mail.active ? "active" : ""}`}>
            <div className={"mail-status"} style={{backgroundColor: mail.color || "transparent"}}></div>

            <div className={"mail-checkbox"}>
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
                {mail.date}
            </div>
        </div>
    )
}

export default MailItem;