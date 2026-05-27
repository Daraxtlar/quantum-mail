import "../styles/Mail-list.css";
import MailItem from "./MailItem.jsx";
import {Archive, Funnel, MoveRight, Trash2, Star, ChevronRight, ChevronLeft} from "lucide-react"
import {useEffect, useState} from "react";


function MailList({mails = [], path = "", onMailClick}) {
    const [selectedMails, setSelectedMails] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const mailsPerPage = 20;

    useEffect(() => {
        setSelectedMails([]);
        setCurrentPage(1);
    }, [mails]);

    const totalPages = Math.ceil(mails.length / mailsPerPage);
    const startIndex = (currentPage - 1) * mailsPerPage;
    const endIndex = startIndex + mailsPerPage;
    const currentMails = mails.slice(startIndex, endIndex);
    const showingFrom = mails.length > 0 ? startIndex + 1 : 0;
    const showingTo = Math.min(endIndex, mails.length);

    const allSelected = mails.length > 0 && selectedMails.length === mails.length;

    const handleSelectAll = () => {
        if (allSelected) {
            setSelectedMails(prev => prev.filter(id => !currentMails.find(m => m.id === id)));
        } else {
            setSelectedMails(prev => [
                ...prev,
                ...currentMails.map(m => m.id).filter(id => !prev.includes(id))
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

    const handleDelete = () => {
        //TODO: Implement delete functionality, send selectedMails to backend for deletion
        setSelectedMails([])
    };

    const handleArchive = () => {
        //TODO: Implement archive functionality, send selectedMails to backend for archiving
        setSelectedMails([])
    };

    const handleStar = () => {
        //TODO: Implement star functionality, send selectedMails to backend for starring
        setSelectedMails([])
    };

    const goToPage = (page) => {
        setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    }


    return (
        <section className={"mail-section"}>
            <div className={"mail-toolbar"}>
                <div className={"toolbar-left"}>
                    {currentMails.length > 0 && (
                        <input type={"checkbox"} checked={allSelected} onChange={handleSelectAll}/>
                    )}

                    {selectedMails.length > 0 && (
                        <div className={"action-buttons"}>
                            <button className={"action-btn"} onClick={handleArchive} title={"Archive"}>
                                <Archive size={16}/>
                            </button>

                            <button className={"action-btn"} onClick={handleDelete} title={"Delete"}>
                                <Trash2 size={16}/>
                            </button>

                            <button className={"action-btn"} onClick={handleStar} title={"Star"}>
                                <Star size={16}/>
                            </button>

                            <button className={"action-btn"} title={"Move"}>
                                <MoveRight size={16}/>
                            </button>
                            <span className={"selected-count"}>{selectedMails.length} selected</span>
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
                {currentMails.length === 0 ? (
                    <div className={"empty-state"}>
                        <p>No messages in this folder</p>
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

            {/* Paginacja na dole */}
            {mails.length > mailsPerPage && (
                <div className={"mail-pagination"}>
                    <span className={"pagination-info"}>
                        {showingFrom}–{showingTo} of {mails.length}
                    </span>

                    <div className={"pagination-buttons"}>
                        <button
                            className={"pagination-btn"}
                            onClick={() => goToPage(currentPage - 1)}
                            disabled={currentPage === 1}
                        >
                            <ChevronLeft size={16} />
                        </button>

                        {/* Numery stron */}
                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(page => {
                                // Pokaż pierwszą, ostatnią i okolice aktualnej
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
