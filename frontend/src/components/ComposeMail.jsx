import "../styles/Compose-mail.css";
import {X, Paperclip, Send, Trash2, Minus, Maximize2} from "lucide-react";
import {useState} from "react";

function ComposeMail({onClose, onSend, userEmail, replyTo}) {
    const [to, setTo] = useState(
        replyTo
            ? replyTo._type === "reply"
                ? replyTo.email
                : ""
            : ""
    );
    const [subject, setSubject] = useState(
        replyTo
            ? replyTo._type === "forward"
                ? `Fwd: ${replyTo.subject}`
                : `Re: ${replyTo.subject}`
            : ""
    );
    const [body, setBody] = useState(
        replyTo && replyTo._type === "forward"
            ? `\n\n---------- Forwarded message ----------\nFrom: ${replyTo.sender} <${replyTo.email}>\nDate: ${replyTo.date}\nSubject: ${replyTo.subject}\n\n${replyTo.body}`
            : ""
    );
    const [files, setFiles] = useState([]);
    const [isMinimized, setIsMinimized] = useState(false);
    const [isMaximized, setIsMaximized] = useState(false);
    const [sending, setSending] = useState(false);

    const handleFileSelect = (e) => {
        const selectedFiles = Array.from(e.target.files);
        setFiles(prev => [...prev, ...selectedFiles]);
    };

    const removeFile = (index) => {
        setFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleSend = () => {
        if (!to.trim()) {
            alert("Recipient email is required.");
            return;
        }

        const mailData = {
            senders: userEmail || "user@quantummail.com",
            recipients: to.split(",").map(email => email.trim()).filter(Boolean),
            subject: subject,
            text: body,
            method: "send",
            files: files,
        };

        if (onSend) {
            onSend(mailData);
        }

        setTo("");
        setSubject("");
        setBody("");
        setFiles([]);
        onClose?.();
    };

    if (isMinimized) {
        return (
            <div className={"compose-minimized"} onClick={() => setIsMinimized(false)}>
                <span>New Message</span>
                <div className={"minimized-actions"}>
                    <button onClick={(e) => {
                        e.stopPropagation();
                        setIsMinimized(false);
                    }}>
                        <Maximize2 size={14}/>
                    </button>
                    <button onClick={(e) => {
                        e.stopPropagation();
                        onClose?.();
                    }}>
                        <X size={14}/>
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className={"compose-overlay"}>
            <div className={`compose-window ${isMaximized ? "fullscreen" : ""}`}>
                <div className={"compose-header"}>
                    <span className={"compose-title"}>New Message</span>
                    <div className={"compose-header-actions"}>
                        <button className={"compose-header-btn"} onClick={() => setIsMinimized(true)}>
                            <Minus size={16}/>
                        </button>
                        <button className={"compose-header-btn"} onClick={() => setIsMaximized(!isMaximized)}>
                            <Maximize2 size={16}/>
                        </button>
                        <button className={"compose-header-btn"} onClick={onClose}>
                            <X size={16}/>
                        </button>
                    </div>
                </div>

                <div className={"compose-body"}>
                    <div className={"compose-field"}>
                        <label className={"compose-label"}>To:</label>
                        <input
                            type={"text"}
                            className={"compose-input"}
                            value={to}
                            onChange={(e) => setTo(e.target.value)}
                            placeholder={"recipient@email.com"}
                            autoFocus
                        />
                    </div>

                    <div className={"compose-field"}>
                        <label className={"compose-label"}>Subject:</label>
                        <input
                            type={"text"}
                            className={"compose-input"}
                            value={subject}
                            onChange={(e) => setSubject(e.target.value)}
                            placeholder={"Email subject"}
                        />
                    </div>

                    {files.length > 0 && (
                        <div className={"attachments-preview"}>
                            {files.map((file, index) => (
                                <div key={index} className={"attachment-tag"}>
                                    <span>{file.name}</span>
                                    <button onClick={() => removeFile(index)}>
                                        <X size={12}/>
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className={"compose-field-body"}>
                        <textarea
                            className={"compose-textarea"}
                            value={body}
                            onChange={(e) => setBody(e.target.value)}
                            placeholder={"Write your message here..."}
                        />
                    </div>
                </div>

                <div className={"compose-footer"}>
                    <button
                        className={"send-btn"}
                        onClick={handleSend}
                        disabled={sending}
                    >
                        <Send size={16}/>
                        {sending ? "Sending..." : "Send"}
                    </button>

                    <label className={"attach-btn"} title={"Attach files"}>
                        <Paperclip size={16}/>
                        <input
                            type={"file"}
                            multiple
                            hidden
                            onChange={handleFileSelect}
                        />
                    </label>

                    <button className={"discard-btn"} onClick={onClose} title={"Discard"}>
                        <Trash2 size={16}/>
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ComposeMail;