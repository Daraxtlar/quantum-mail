import Topbar from "../components/Topbar.jsx";
import Sidebar from "../components/Sidebar.jsx";
import MailList from "../components/MailList.jsx";
import "../styles/Inbox.css"
import {useEffect, useState} from "react";
import MailDetail from "../components/MailDetail.jsx";
import ComposeMail from "../components/ComposeMail.jsx";
import {mailService} from "../services/MailService.js";


//Test Konta i Foldery
const accounts = [
    {
        id: 1,
        email: "JohnSmith@gmail.com",
        folders: [
            {name: "Inbox", unread: 3},
            {name: "Drafts"},
            {name: "Sent"},
            {name: "Starred"},
            {name: "Spam", unread: 2},
            {name: "Trash"},
        ],
    },
    {
        id: 2,
        email: "AuthorJohnSmith@gmail.com",
        folders: [
            {name: "Inbox", unread: 1},
            {name: "Drafts"},
            {name: "Sent"},
            {name: "Starred"},
            {name: "Spam"},
            {name: "Trash", unread: 3},
        ],
    },
    {
        id: 3,
        email: "Love2Write@yahoo.com",
        folders: [],
    },
    {
        id: 4,
        email: "JohnSmith@gmail.com",
        folders: [
            {name: "Inbox", unread: 3},
            {name: "Drafts"},
            {name: "Sent"},
            {name: "Starred"},
            {name: "Spam", unread: 2},
            {name: "Trash"},
        ],
    },
    {
        id: 5,
        email: "JohnSmith@gmail.com",
        folders: [
            {name: "Inbox", unread: 3},
            {name: "Drafts"},
            {name: "Sent"},
            {name: "Starred"},
            {name: "Spam", unread: 2},
            {name: "Trash"},
        ],
    },
];

function Inbox() {
    const [currentAccount, setCurrentAccount] = useState("lukasz78899@op.pl");
    const [currentFolder, setCurrentFolder] = useState("INBOX");
    const [selectedMail, setSelectedMail] = useState(null);
    const [showCompose, setShowCompose] = useState(false);
    const [replyMail, setReplyMail] = useState(null);
    const [initialToEmail, setInitialToEmail] = useState("");


    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalCount, setTotalCount] = useState(0);


    const [searchQuery, setSearchQuery] = useState("");
    const [mails, setMails] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (currentAccount){
            loadEmails();
        }
    }, [currentAccount, currentFolder, currentPage, searchQuery]);

    const updateMailState = (response) => {
        const formattedMails = response.emails.map(email => ({
            id: email.uid,
            folderName: email.folderName,
            sender: email.sender || "Unknown",
            email: email.sender || "",
            subject: email.subject || "(no subject)",
            preview: email.snippet || "",
            body: email.content || "",
            date: formatDate(email.sentDate),
            read: email.read || false,
            attachments: email.attachments || [],
            color: !email.read ? "#7CFF5B" : undefined,
        }));
        setMails(formattedMails);
        setTotalPages(response.totalPages);
        setTotalCount(response.totalCount);
    }

    const loadEmails = async () => {
        try{
            const pageForBackend = currentPage - 1;

            if (mails.length === 0) setLoading(true);

            const response = await mailService.fetchEmails(currentAccount ,currentFolder, pageForBackend, 20, searchQuery);
            updateMailState(response);
            setLoading(false);

            if (currentPage === 1 && (!searchQuery || searchQuery.trim() === "")){
                mailService.syncEmails(currentAccount, currentFolder)
                    .then(async () => {
                        console.log("Sync completed, reloading emails...");
                        const updatedResponse = await mailService.fetchEmails(currentAccount ,currentFolder, pageForBackend, 20);
                        updateMailState(updatedResponse);
                    })
                    .catch (error => console.error("Error during sync:", error));
            }
        }catch (error){
            console.error("Error fetching emails:", error);
            setLoading(false);
        }
    };

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

    const getCurrentMails = () => mails;

    const handleMailClick = async (mail) => {
        setLoading(true);
        try{
            const targetFolder = mail.folderName || currentFolder;
            const fullMail = await mailService.fetchEmailDetails(currentAccount, targetFolder, mail.id);

            const formattedDetail = {
                ...fullMail,
                sender: fullMail.from,
                content: fullMail.content,
                date: formatDate(fullMail.sentDate),
                color: undefined
            };
            setSelectedMail(formattedDetail);

            setMails(prevMails => prevMails.map(m => m.id === mail.id ? {...m, read: true, color: undefined} : m));
            
        }catch (error){
            console.error("Error fetching email details:", error);
            alert("Failed to load email details. Please try again.");
        }finally {
            setLoading(false);
        }
    };

    const handleFolderClick = (accountId, folderName) => {
        setCurrentPage(1);
        setSelectedMail(null);

        if (accountId && folderName){
            const account = accounts.find(acc => acc.id === accountId);
            if (account){
                setCurrentAccount(account.email);
                const mappedFolder = folderName.toUpperCase() === "INBOX" ? "INBOX" : folderName;
                setCurrentFolder(mappedFolder);
            }
        }
    };

    const openCompose = (email = "") => {
        setReplyMail(null);
        setInitialToEmail(typeof email === "string" ? email : "");
        setShowCompose(true);
    };

    const replyToMail = (mail, type = "reply") => {
        setInitialToEmail("");
        setReplyMail({ ...mail, _type: type });
        setShowCompose(true);
    };

    const closeCompose = () => {
        setShowCompose(false);
        setReplyMail(null);
        setInitialToEmail("");
    };

    const currentMails = getCurrentMails();
    const path = currentAccount && currentFolder ? `/${currentAccount}/${currentFolder}` : "";

    return (
        <div className={"inbox"}>
            <Topbar onCompose={openCompose} onSearch={setSearchQuery}/>
            <div className={"layout"}>
                <Sidebar
                    accounts={accounts}
                    onFolderClick={handleFolderClick} />
                {selectedMail ? (
                    <MailDetail
                        mail={selectedMail}
                        onBack={() => setSelectedMail(null)}
                        onReply={replyToMail}
                        folder={currentFolder}
                    />
                ) : (
                    <MailList
                        mails = {currentMails}
                        searchQuery={searchQuery}
                        path= {path}
                        onMailClick={handleMailClick}
                        currentPage={currentPage}
                        totalPages = {totalPages}
                        onPageChange={setCurrentPage}
                        loading={loading}
                    />
                )}
            </div>

            {showCompose && (
                <ComposeMail
                    onClose={closeCompose}
                    userEmail={currentAccount || "user@quantummail.com"}
                    replyTo={replyMail}
                    folder={currentFolder}
                    initialTo={initialToEmail}
                />
            )}
        </div>
    )
}

export default Inbox;