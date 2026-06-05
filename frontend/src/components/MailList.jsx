import "../styles/Mail-list.css";
import MailItem from "./MailItem.jsx";
import {Archive, Funnel, MoveRight, Trash2, Star, ChevronRight, ChevronLeft, Loader2} from "lucide-react"
import {useEffect, useState} from "react";


function MailList({mails = [], searchQuery="",path = "", onMailClick, currentPage, totalPages= 1,onPageChange, loading}) {
    const [selectedMails, setSelectedMails] = useState([]);

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
    }, [mails]);

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
        onPageChange?.(page);
    }


    return (
        <section className={"mail-section"}>
            <div className={"mail-toolbar"}>
                <div className={"toolbar-left"}>
                    {currentMails.length > 0 && (
                        <input type={"checkbox"} checked={allCurrentSelected} onChange={handleSelectAll}/>
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
