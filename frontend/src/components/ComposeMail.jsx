import "../styles/Compose-mail.css";
import {X, Paperclip, Send, Trash2, Minus, Maximize2} from "lucide-react";
import {useCallback, useEffect, useRef, useState} from "react";
import {mailService} from "../services/MailService.js";
import PopupAlert from "./PopupAlert.jsx";

function ComposeMail({onClose, userEmail, replyTo, folder, initialTo}) {
    const [availableSenders, setAvailableSenders] = useState([]);
    const [senderEmail, setSenderEmail] = useState(userEmail);
    const [suggestions, setSuggestions] = useState([]);
    const [alert, setAlert] = useState(null);

    const [to, setTo] = useState(
        initialTo || (replyTo
            ? replyTo._type === "reply"
                ? replyTo.sender
                : ""
            : "")
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
            ? `\n\n---------- Forwarded message ----------\n\n`
            : ""
    );

    useEffect(() => {
        mailService.fetchUserEmailAccounts()
            .then(accounts => {
                setAvailableSenders(accounts);

                if (!userEmail && accounts.length > 0){
                    setSenderEmail(accounts[0]);
                }
            })
            .catch(err => {
                console.error("Error fetching user email accounts:", err);
            })
    }, [])

    useEffect(() => {
        if (senderEmail){
            mailService.fetchSuggestions(senderEmail)
                .then(data => setSuggestions(data))
                .catch(err => {
                    console.error("Error fetching suggestions:", err);
                });
        }
    }, [senderEmail])

    useEffect(() => {
        setTo(
            replyTo
                ? replyTo._type === "reply"
                    ? replyTo.sender
                    : ""
                : initialTo || ""
        );
        setSubject(
            replyTo
                ? replyTo._type === "forward"
                    ? `Fwd: ${replyTo.subject}`
                    : `Re: ${replyTo.subject}`
                : ""
        );
        setBody(
            replyTo && replyTo._type === "forward"
                ? `\n\n---------- Forwarded message ----------\n\n`
                : ""
        );
        setFiles([]);
    }, [replyTo, initialTo]);

    const getTitle = (isMinimized = false) => {
        const maxLength = isMinimized ? 30 : Math.floor((size.width || 560) / 12);

        let title = "New Message";
        if (replyTo) {
            if (replyTo._type === "reply") {
                title = `Reply to ${replyTo.sender}`;
            } else if (replyTo._type === "forward") {
                title = `Forward: ${replyTo.subject}`;
            }
        }

        return title.length > maxLength ? title.substring(0, maxLength - 3) + "..." : title;
    } ;

    const [files, setFiles] = useState([]);
    const [sending, setSending] = useState(false);
    const [isMinimized, setIsMinimized] = useState(false);
    const [isDetached, setIsDetached] = useState(false);

    const [position, setPosition] = useState({x:0, y:0});
    const [isDragging, setIsDragging] = useState(false);
    const dragStart = useRef({x: 0, y: 0});

    const [size, setSize] =  useState({width: 560, height:"auto"});
    const [isResizing, setIsResizing] = useState(false);
    const resizeStart = useRef({x: 0, y: 0, width: 0, height: 0});
    const windowRef = useRef(null);

    const handleMouseDown = (e) => {
        if (e.target.closest('.compose-header-actions')) return;
        setIsDragging(true);
        dragStart.current = {
            x: e.clientX - position.x,
            y: e.clientY - position.y
        };
    };

    const handleMouseMove = useCallback((e) => {
        if (isDragging){
            setPosition({
                x: Math.max(0, Math.min(e.clientX - dragStart.current.x, window.innerWidth - (size.width || 560))),
                y: Math.max(0, Math.min(e.clientY - dragStart.current.y, window.innerHeight - 60))
            });
        }

        if (isResizing && windowRef.current){
            const dx = e.clientX - resizeStart.current.x;
            const dy = e.clientY - resizeStart.current.y;

            let newWidth = resizeStart.current.width;
            let newHeight = resizeStart.current.height;

            if (resizeStart.current.direction === 'right' || resizeStart.current.direction === 'corner'){
                newWidth = Math.max(400, Math.min(resizeStart.current.width + dx, window.innerWidth - position.x));
            }
            if (resizeStart.current.direction === 'bottom' || resizeStart.current.direction === 'corner'){
                newHeight = Math.max(300, Math.min(resizeStart.current.height + dy, window.innerHeight - position.y));
            }
            setSize({width: newWidth, height: newHeight});
        }
    }, [isDragging, isResizing, position.x, position.y, size.width]);

    const handleMouseUp = useCallback(() => {
        setIsDragging(false);
        setIsResizing(false);
    }, []);

    useEffect(() => {
        if (isDragging || isResizing){
            window.addEventListener('mousemove', handleMouseMove);
            window.addEventListener('mouseup', handleMouseUp);
            document.body.style.userSelect = 'none';
            document.body.style.pointerEvents = 'none';
        }
        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
            document.body.style.userSelect = '';
            document.body.style.pointerEvents = 'auto';
        };
    }, [isDragging, isResizing, handleMouseMove, handleMouseUp]);

    const handleResizeStart = (e, direction) => {
        e.stopPropagation();
        setIsResizing(true);
        const rect = windowRef.current.getBoundingClientRect();
        resizeStart.current = {
            x: e.clientX,
            y: e.clientY,
            width: rect.width,
            height: rect.height,
            direction: direction
        };
    };

    const handleDetach = () => {
        setIsDetached(!isDetached);
        if (!isDetached){
            setPosition({
                x: window.innerWidth / 2 - 400,
                y: window.innerHeight / 2 - 300
            });
            setSize({width: 800, height: 600});
        } else {
            setPosition({x: 0, y: 0});
            setSize({width: 560, height: "auto"});
        }
    };

    const handleFileSelect = (e) => {
        const selectedFiles = Array.from(e.target.files);
        setFiles(prev => [...prev, ...selectedFiles]);
    };

    const removeFile = (index) => {
        setFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleSend = async () => {
        if (!to.trim()) {
            setAlert({
                message: "Recipient email is required.",
                type: "error",
                action: () => setAlert(null)
            })
            return;
        }

        setSending(true);

        const formData = new FormData();
        formData.append('senders', senderEmail);

        const recipients = to.split(",").map(email => email.trim()).filter(Boolean);
        recipients.forEach(recipient => formData.append('recipients', recipient));

        formData.append('subject', subject);
        formData.append('method', 'send');
        formData.append('text', body);

        if (replyTo){
            formData.append('parentMailId', replyTo.id);
            formData.append('folderName', folder || "INBOX");
            formData.append('actionType', replyTo._type);
        }

        if (files.length > 0){
            files.forEach(file => formData.append('files', file));
        }

        try{
            await mailService.sendEmail(formData);

            setTo("");
            setSubject("");
            setBody("");
            setFiles([]);
            setAlert({
                message: "Message sent successfully!",
                type: "success",
                action: () => {
                    setAlert(null);
                    onClose?.();
                }
            })
        }catch (error) {
            console.error("Error sending email:", error);

            const errorMessage = error.response?.data?.message || "Failed to send the email. Please try again.";
            setAlert({
                message: errorMessage,
                type: "error",
                action: () => setAlert(null)
            });
        } finally {
            setSending(false);
        }
    };

    if (isMinimized) {
        return (
            <div className={"compose-minimized"} onClick={() => setIsMinimized(false)}>
                <span>{getTitle(true)}</span>
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

    const windowStyle = isDetached ? {
        position: 'fixed',
        left: position.x,
        top: position.y,
        width: size.width,
        height: size.height,
        borderRadius: 12,
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6)',
    } : {};

    return (
        <div className={"compose-overlay"}>
            {alert && (
                <PopupAlert
                    message={alert.message}
                    type={alert.type}
                    onClose={alert.action || (() => setAlert(null))}
                />
            )}

            <div ref={windowRef} className={`compose-window ${isDetached ? "detached" : ""}`} style={windowStyle}>
                <div className={`compose-header ${isDetached ? "draggable" : ""}`} onMouseDown={isDetached ? handleMouseDown : undefined}>
                    <span className={"compose-title"}>{getTitle()}</span>
                    <div className={"compose-header-actions"}>
                        <button className={"compose-header-btn"} onClick={() => setIsMinimized(true)}>
                            <Minus size={16}/>
                        </button>
                        <button className={"compose-header-btn"} onClick={handleDetach}>
                            <Maximize2 size={16}/>
                        </button>
                        <button className={"compose-header-btn"} onClick={onClose}>
                            <X size={16}/>
                        </button>
                    </div>
                </div>

                <div className={"compose-body"}>
                    <div className={"compose-field"}>
                        <label className={"compose-label"}>From:</label>
                        <select
                            className={"compose-input"}
                            value={senderEmail}
                            onChange={(e) => setSenderEmail(e.target.value)}
                        >
                            {availableSenders.map((email, idx) => (
                                <option key={idx} value={email}>{email}</option>
                            ))}
                        </select>
                    </div>

                    <div className={"compose-field"}>
                        <label className={"compose-label"}>To:</label>
                        <input
                            type={"text"}
                            className={"compose-input"}
                            value={to}
                            onChange={(e) => setTo(e.target.value)}
                            placeholder={"recipient@email.com"}
                            list={"recent-recipients"}
                            autoFocus
                        />
                        <datalist id={"recent-recipients"}>
                            {suggestions.map((email, index) => (
                                <option key={index} value={email}/>
                            ))}
                        </datalist>
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

                {isDetached && (
                    <>
                        <div className={"resize-handle right"} onMouseDown={(e) => handleResizeStart(e, 'right')} />
                        <div className={"resize-handle bottom"} onMouseDown={(e) => handleResizeStart(e, 'bottom')} />
                        <div className={"resize-handle corner"} onMouseDown={(e) => handleResizeStart(e, 'corner')} />
                    </>
                )}
            </div>
        </div>
    );
}

export default ComposeMail;