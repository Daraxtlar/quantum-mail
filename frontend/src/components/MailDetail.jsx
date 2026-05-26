import "../styles/Mail-detail.css";
import { ArrowLeft, Archive, Trash2, Star, Reply, Forward, MoreHorizontal, Paperclip, Download} from "lucide-react";

function MailDetail({mail, onBack, onReply}) {
    if (!mail) return null;

    return (
        <div className={"mail-detail"}>
            <div className={"detail-toolbar"}>
                <button className={"detail-btn"} onClick={onBack}>
                    <ArrowLeft size={18} />
                </button>
                <div className={"detail-actions"}>
                    <button className={"detail-btn"}>
                        <Archive size={18} />
                    </button>
                    <button className={"detail-btn"}>
                        <Trash2 size={18} />
                    </button>
                    <button className={"detail-btn"}>
                        <Star size={18} />
                    </button>
                    <button className={"detail-btn"}>
                        <MoreHorizontal size={18} />
                    </button>
                </div>
            </div>

            <div className={"detail-content"}>
                <h2 className={"detail-subject"}>{mail.subject}</h2>

                <div className={"detail-sender-info"}>
                    <div className={"sender-avatar"} style={{backgroundColor: mail.color || "#1540ff"}}>
                        {mail.sender.charAt(0).toUpperCase()}
                    </div>
                    <div className={"sender-details"}>
                        <div className={"sender-name"}>{mail.sender}</div>
                        <div className={"sender-email"}>{mail.email || "unkown@email.com"}</div>
                    </div>
                    <div className={"detail-date"}>{mail.date}</div>
                </div>

                <div className={"detail-body"}>
                    {mail.body?.split('\n').map((line, index) => (
                            <p key={index}>{line || '\u00A0'}</p>
                        ))}
                </div>

                {mail.attachments && mail.attachments.length > 0 && (
                    <div className={"detail-attachments-section"}>
                        <h3 className={"attachments-title"}>
                            <Paperclip size={16} />
                            Attachments ({mail.attachments.length})
                        </h3>
                        <div className={"attachments-list"}>
                            {mail.attachments.map((file, index) => (
                                <div key={index} className={"attachment-item"}>
                                    <div className={"attachment-name"}>{file}</div>
                                    <button className={"download-btn"} title={"Download"}>
                                        <Download size={14} />
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                    )}

                <div className={"detail-reply-actions"}>
                    <button className={"reply-btn"} onClick={() => onReply?.(mail, "reply")}>
                        <Reply size={16} />
                        Reply
                    </button>
                    <button className={"reply-btn"} onClick={() => onReply?.(mail, "forward")}>
                        <Forward size={16} />
                        Forward
                    </button>
                </div>
            </div>
        </div>
    );
}

export default MailDetail;