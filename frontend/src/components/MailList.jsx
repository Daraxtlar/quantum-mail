import "../styles/Mail-list.css";
import MailItem from "./MailItem.jsx";
import {Funnel, MoveRight, Trash2, Star, ChevronRight, ChevronLeft, Loader2} from "lucide-react"
import { useEffect, useRef, useState} from "react";
import {mailService} from "../services/MailService.js";
import PopupAlert from "./PopupAlert.jsx";


function MailList({
                      mails = [],
                      searchQuery="",
                      path = "",
                      onMailClick,
                      currentPage,
                      totalPages= 1,
                      onPageChange,
                      loading,
                      folder = "INBOX",
                      accountEmail = "",
                      onRefresh}) {
    const [selectedMails, setSelectedMails] = useState([]);
    const [isMoveMenuOpen, setIsMoveMenuOpen] = useState(false);
    const [isActionLoading, setIsActionLoading] = useState(false);
    const dropdownRef = useRef(null);

    const [alert, setAlert] = useState(null);

    const availableFolders = ["INBOX", "DRAFTS", "SENT", "STARRED", "SPAM", "TRASH"];

    const currentMails = searchQuery.trim()
        ? mails.filter(mail =>
            mail.subject?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            mail.sender?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            mail.body?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            mail.preview?.toLowerCase().includes(searchQuery.toLowerCase())
        )
        : mails;

    useEffect(() => {
        setSelectedMails([]);
    }, [folder, currentPage, searchQuery]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsMoveMenuOpen(false);
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, []);

    const allCurrentSelected = currentMails.length > 0 && currentMails.every(m => selectedMails.includes(m.id));


    useEffect(() => {
        if (searchQuery && searchQuery.trim()){
            onPageChange?.(1);
        }
    }, [searchQuery]);


    const handleSelectAll = () => {
        if (allCurrentSelected) {
            setSelectedMails(prev => prev.filter(id => !currentMails.find(m => m.id === id)));
        } else {
            setSelectedMails(prev => [
                ...prev.filter(id => !currentMails.find(m => m.id === id)),
                ...currentMails.map(m => m.id)
            ]);
        }
    };

    const handleToggleMail = (mailId) => {
        setSelectedMails(prev => {
            if (prev.includes(mailId)) {
                return prev.filter(id => id !== mailId);
            } else {
                return [...prev, mailId];
            }
        });
    };

    const executeMoveMails = async (targetFolder) => {
        if (!accountEmail || selectedMails.length === 0) return;

        setIsActionLoading(true);
        const count = selectedMails.length;

        try{
            const mailsToMove = mails.filter(m => selectedMails.includes(m.id));
            await Promise.all(
                mailsToMove.map(mail =>
                mailService.moveMail(accountEmail, folder, targetFolder, mail.id, mail.sender, mail.subject, mail.date)
                )
            );

            setSelectedMails([]);
            setIsMoveMenuOpen(false);
            onRefresh?.();
            setAlert({
                message: `Successfully moved ${count} ${count === 1 ? 'message' : 'messages'} to ${targetFolder}`,
                type: "success",
                action: () => setAlert(null)
            })

    }catch (err) {
            console.error("Error moving mails: ", err);
            setAlert({
                message: `Failed to move messages: ${err.message}`,
                type: "error",
                action: () => setAlert(null)
            })
        }finally {
            setIsActionLoading(false);
        }
    };

    const handleDelete = () => {
        executeMoveMails("TRASH");
    };

    const handleStar = () => {
        executeMoveMails("STARRED");
    };

    const goToPage = (page) => {
        onPageChange?.(page);
    }


    return (
        <section className={"mail-section"}>
            {alert && (
                <PopupAlert
                    message={alert.message}
                    type={alert.type}
                    onClose={alert.action}
                />
            )}
            <div className={"mail-toolbar"}>
                <div className={"toolbar-left"}>
                    {currentMails.length > 0 && (
                        <input type={"checkbox"} checked={allCurrentSelected} onChange={handleSelectAll} disabled={isActionLoading}/>
                    )}

                    {selectedMails.length > 0 && (
                        <div className={`action-buttons ${isActionLoading ? "opacity-50 pointer-events-none" : ""}`}>

                            <button className={"action-btn"} onClick={handleDelete} title={"Delete"} disabled={isActionLoading}>
                                <Trash2 size={16}/>
                            </button>

                            <button className={"action-btn"} onClick={handleStar} title={"Star"} disabled={isActionLoading}>
                                <Star size={16}/>
                            </button>

                            <div className="move-dropdown-container" ref={dropdownRef}>
                                <button
                                    className={`action-btn ${isMoveMenuOpen ? "active" : ""}`}
                                    onClick={() => setIsMoveMenuOpen(!isMoveMenuOpen)}
                                    title={"Move to folder"}
                                    disabled={isActionLoading}
                                >
                                    <MoveRight size={16}/>
                                </button>

                                {isMoveMenuOpen && (
                                    <div className="move-dropdown-menu">
                                        {availableFolders
                                            .filter(f => f !== folder.toUpperCase())
                                            .map(f => (
                                                <div
                                                    key={f}
                                                    className="dropdown-item"
                                                    onClick={() => executeMoveMails(f)}
                                                >
                                                    {f}
                                                </div>
                                            ))
                                        }
                                    </div>
                                )}
                            </div>

                            <span className={"selected-count"}>
                                {isActionLoading ? "Moving..." : `${selectedMails.length} selected`}
                            </span>
                        </div>
                    )}
                </div>

                <div className={"toolbar-right"}>
                    <span className={"path-text"}>{path}</span>

                    <button className={"filter-btn"}>
                        <Funnel size={16}/>
                    </button>
                    <span>everything</span>
                </div>
            </div>

            <div className={"mail-list"}>
                {loading ? (
                    <div className={"loading-state"}>
                        <div className={"spinner"}><Loader2 className="animate-spin text-blue-500" size={48} /></div>
                        <p>Loading messages...</p>
                    </div>
                ) : currentMails.length === 0 ? (
                    <div className={"empty-state"}>
                        <p>{searchQuery ? "No results found" : "No messages in this folder"}</p>
                    </div>
                ) : (
                    currentMails.map(mail => (
                        <MailItem
                            key={mail.id}
                            mail={mail}
                            isSelected={selectedMails.includes(mail.id)}
                            onToggle={() => handleToggleMail(mail.id)}
                            onClick={() => onMailClick?.(mail)}
                        />
                    ))
                )}
            </div>

            {totalPages > 1 && (
                <div className={"mail-pagination"}>
                    <span className={"pagination-info"}>
                        Page {currentPage} of {totalPages}
                    </span>

                    <div className={"pagination-buttons"}>
                        <button
                            className={"pagination-btn"}
                            onClick={() => goToPage(currentPage - 1)}
                            disabled={currentPage === 1}
                        >
                            <ChevronLeft size={16} />
                        </button>

                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(page => {
                                return page === 1 ||
                                    page === totalPages ||
                                    Math.abs(page - currentPage) <= 1;
                            })
                            .map((page, index, arr) => (
                                <span key={`page-${page}`}>
                                    {index > 0 && arr[index - 1] !== page - 1 && (
                                        <span className={"pagination-dots"}>...</span>
                                    )}
                                    <button
                                        className={`pagination-num ${page === currentPage ? "active" : ""}`}
                                        onClick={() => goToPage(page)}
                                    >
                                        {page}
                                    </button>
                                </span>
                            ))
                        }

                        <button
                            className={"pagination-btn"}
                            onClick={() => goToPage(currentPage + 1)}
                            disabled={currentPage === totalPages}
                        >
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            )}
        </section>
    );
}

export default MailList;
