import "../styles/Mail-detail.css";
import { ArrowLeft, Archive, Trash2, Star, Reply, Forward, MoreHorizontal, Paperclip, Download} from "lucide-react";
import DOMPurify from "dompurify";
import {useRef} from "react";

function MailDetail({mail, onBack, onReply}) {
    if (!mail) return null;
    const iframeRef = useRef(null);
    const initial = mail.sender?.charAt(0).toUpperCase() || "?";

    const prepareHtml = (rawHtml) => {
        if (!rawHtml) return "";

        let processed = DOMPurify.sanitize(rawHtml, {
            ADD_TAGS: ["style", "meta"],
            ADD_ATTR: ["target", "style", "cid", "src"],
            FORCE_BODY: true
        });

        processed = processed.replace(/src=['"]cid:([^'"]+)['"]/gi, (match, cidName) => {
            const safeCid = encodeURIComponent(cidName);
            return `src="http://localhost:8080/api/mails/${mail.id}/attachments/${safeCid}"`;
        });

        processed = processed.replace(/data-src=/gi, 'src=');

        return `
        <!DOCTYPE html>
        <html>
            <head>
                <style>
                    body { font-family: sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    img { max-width: 100% !important; height: auto !important; display: block; }
                    table { width: 100% !important; }
                </style>
            </head>
            <body>${processed}</body>
        </html>
    `;
    };

    const updateIframeHeight = () => {
        const iframe = iframeRef.current;
        if (iframe && iframe.contentWindow) {
            iframe.style.height = "100px";

            const height = iframe.contentWindow.document.documentElement.scrollHeight;
            iframe.style.height = height + "px";
        }
    };


    const handleDownload = (fileName) => {
        window.open(`http://localhost:8080/api/mails/${mail.id}/attachments/${fileName}?download=true`, "_blank")
    }

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
                        {initial}
                    </div>
                    <div className={"sender-details"}>
                        <div className={"sender-name"}>{mail.sender}</div>
                        <div className={"sender-email"}>{mail.email || "unknown@email.com"}</div>
                    </div>
                    <div className={"detail-date"}>{mail.date}</div>
                </div>

                <div className={"detail-body"}>
                    {mail.content ? (
                        <iframe
                            ref={iframeRef}
                            title="email-body"
                            srcDoc={prepareHtml(mail.content)}
                            onLoad={updateIframeHeight}
                            style={{
                                width: '100%',
                                border: 'none',
                                overflow: 'hidden',
                                transition: 'height 0.2s ease-in-out'
                            }}
                            sandbox="allow-same-origin allow-popups allow-popups-to-escape-sandbox"
                        />
                    ) : (
                        <p className="no-content-msg">Brak treści do wyświetlenia.</p>
                    )}
                </div>

                {mail.attachments && mail.attachments.length > 0 && (
                    <div className={"attachments-section"}>
                        <h3 className={"attachments-title"}>
                            <Paperclip size={16} />
                            Attachments ({mail.attachments.length})
                        </h3>
                        <div className={"attachments-list"}>
                            {mail.attachments.map((file, index) => (
                                <div key={index} className={"attachment-item"}>
                                    <div className={"attachment-name"}>{file.fileName || file}</div>
                                    <button className={"download-btn"} title={"Download"} onClick={() => handleDownload(file.fileName || file)}>
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