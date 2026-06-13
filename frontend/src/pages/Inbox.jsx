import Topbar from "../components/Topbar.jsx";
import Sidebar from "../components/Sidebar.jsx";
import MailList from "../components/MailList.jsx";
import "../styles/Inbox.css"
import {useEffect, useRef, useState} from "react";
import MailDetail from "../components/MailDetail.jsx";
import ComposeMail from "../components/ComposeMail.jsx";
import {mailService} from "../services/MailService.js";
import {EmailAccountService} from "../services/EmailAccountService.js";

function Inbox() {
    const [accounts, setAccounts] = useState([]);
    const [currentAccount, setCurrentAccount] = useState("");
    const [currentFolder, setCurrentFolder] = useState("INBOX");
    const [selectedMail, setSelectedMail] = useState(null);
    const [showCompose, setShowCompose] = useState(false);
    const [replyMail, setReplyMail] = useState(null);
    const [initialToEmail, setInitialToEmail] = useState("");
    const [isSyncing, setIsSyncing] = useState(false);
    const debounceTimeoutRef = useRef(null);
    const [refreshTrigger, setRefreshTrigger] = useState(0);


    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalCount, setTotalCount] = useState(0);


    const [searchQuery, setSearchQuery] = useState("");
    const [mails, setMails] = useState([]);
    const [loading, setLoading] = useState(false);

    const loadAccounts = async  () => {
        try {
            const data = await EmailAccountService.fetchAccounts();

            const formattedAccounts = data.map(acc => ({
                id: acc.id,
                email: acc.emailAddress,
                folders: [
                    {name: "Inbox"},
                    {name: "Drafts"},
                    {name: "Sent"},
                    {name: "Starred"},
                    {name: "Spam"},
                    {name: "Trash"},
                ],
            }));

            setAccounts(formattedAccounts);

            if (formattedAccounts.length > 0 && !currentAccount){
                setCurrentAccount(formattedAccounts[0].email);
            }
        }catch (error) {
            console.error("Error loading accounts:", error);
            alert("Failed to load email accounts. Please refresh the page.");
        }
    }

    useEffect(() => {
        loadAccounts();
    }, []);


    useEffect(() => {
        let isCurrent = true;
        const abortController = new AbortController();

        if (!currentAccount) return;

        const loadEmails = async () => {
            try{
                const pageForBackend = currentPage - 1;

                if (mails.length === 0) setLoading(true);

                const response = await mailService.fetchEmails(
                    currentAccount ,
                    currentFolder,
                    pageForBackend,
                    20,
                    searchQuery,
                    {signal: abortController.signal }
                );

                if (!isCurrent) return;

                updateMailState(response);
                setLoading(false);

                if (currentPage === 1 && (!searchQuery || searchQuery.trim() === "")){
                    if (debounceTimeoutRef.current){
                        clearTimeout(debounceTimeoutRef.current);
                    }

                    debounceTimeoutRef.current = setTimeout(async () => {
                        if (!isCurrent) return;
                        setIsSyncing(true);

                        try {
                            await mailService.syncEmails(currentAccount, currentFolder);
                            if (!isCurrent) return;

                            const updatedResponse = await mailService.fetchEmails(
                                currentAccount,
                                currentFolder,
                                pageForBackend,
                                20,
                                searchQuery || "",
                                {signal: abortController.signal}
                            );
                            if (!isCurrent) return;
                            updateMailState(updatedResponse);
                        }catch (error){
                            if (error.name !== 'AbortError' && error.name !== 'CanceledError') {
                                console.error("Error syncing emails:", error);
                            }
                        }finally {
                            setIsSyncing(false);
                        }
                    }, 500);
                }
            }catch (error){
                if (error.name !== 'AbortError' && error.name !== 'CanceledError') {
                    console.error("Error fetching emails:", error);
                    if (isCurrent) setLoading(false);
                }
            }
        };

        loadEmails();

        return () => {
            isCurrent = false;
            abortController.abort();
            if (debounceTimeoutRef.current){
                clearTimeout(debounceTimeoutRef.current);
            }
        }
    }, [currentAccount, currentFolder, currentPage, searchQuery, refreshTrigger]);

    const updateMailState = (response) => {
        const formattedMails = response.emails.map(email => ({
            id: email.uid,
            folderName: email.folderName,
            sender: email.sender || "Unknown",
            email: email.sender || "",
            subject: email.subject || "(no subject)",
            preview: email.snippet || "",
            body: email.content || "",
            date: email.sentDate,
            read: email.read || false,
            attachments: email.attachments || [],
            color: !email.read ? "#7CFF5B" : undefined,
        }));
        setMails(formattedMails);
        setTotalPages(response.totalPages);
        setTotalCount(response.totalCount);
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
                date: fullMail.sentDate,
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
        if (!accountId || !folderName) return;

        const account = accounts.find(acc => acc.id === accountId);
        if (!account) return;

        const targetFolder = folderName.toUpperCase();
        const targetAccount = account.email;

        if (currentAccount === targetAccount && currentFolder === targetFolder){
            setSelectedMail(null);
            if (currentPage === 1){
                return;
            }
        }

        setCurrentPage(1);
        setSelectedMail(null);
        setMails([]);

        setCurrentAccount(targetAccount);
        setCurrentFolder(targetFolder);
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
                    onFolderClick={handleFolderClick}
                    onAccountAdded={loadAccounts}
                />
                {selectedMail ? (
                    <MailDetail
                        mail={selectedMail}
                        onBack={() => setSelectedMail(null)}
                        onReply={replyToMail}
                        folder={currentFolder}
                        accountEmail={currentAccount}
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
                        folder={currentFolder}
                        accountEmail={currentAccount}
                        onRefresh={() => setRefreshTrigger(prev => prev + 1)}
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