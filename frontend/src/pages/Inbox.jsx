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

//Test Maili
const allMails = {
    "JohnSmith@gmail.com": {
        Inbox: [
            {
                id: 1,
                sender: "Laura Shea",
                email: "laura.shea@company.com",
                subject: "Closing the deal",
                preview: "This is a snippet of text, it'll show the preview of content inside...",
                body: "Hi John,\n\nI wanted to follow up on our conversation about closing the deal with the client. Everything looks good on our end and we're ready to proceed.\n\nThe contract has been reviewed by legal and they've given us the green light. I've attached the final version for your reference.\n\nLet me know if you have any questions or if there's anything else we need to address before we finalize.\n\nBest regards,\nLaura",
                date: "Yesterday",
                color: "#7CFF5B",
                attachments: ["contract_final.pdf"],
                read: false,
            },
            {
                id: 2,
                sender: "Jonathan, Brett",
                email: "jonathan.brett@startup.io",
                subject: "Start screen & next step",
                preview: "This is a snippet of text, it'll show the preview of content...",
                body: "Hey team,\n\nGreat work on the start screen design! I've reviewed the mockups and have some feedback:\n\n1. The color scheme works well\n2. Maybe we should adjust the spacing between elements\n3. The CTA button could be more prominent\n\nLet's schedule a quick call to discuss the next steps.\n\nCheers,\nJonathan",
                date: "Yesterday",
                read: true,
            },
        ],
        Sent: [
            {
                id: 3,
                sender: "Me",
                email: "johnsmith@gmail.com",
                subject: "RE: Project update",
                preview: "Thanks for the update, I'll review it today...",
                body: "Hi Mark,\n\nThanks for the update. I'll review everything today and get back to you with my comments by EOD.\n\nThe timeline looks good, but I want to double-check the resource allocation before we commit.\n\nBest,\nJohn",
                date: "2 days ago",
                read: true,
            },
        ],
        Drafts: [
            {
                id: 4,
                sender: "Draft",
                email: "",
                subject: "Meeting notes",
                preview: "Notes from today's meeting...",
                body: "Meeting Notes - 15/01/2025\n\nAttendees: John, Sarah, Mike\n\nAgenda:\n1. Q1 Goals Review\n2. Budget Planning\n3. New Hires\n\nNotes:\n- Q1 targets achieved\n- Budget approved for Q2\n- Need to hire 2 developers\n\nAction items:\n- Sarah to prepare job descriptions\n- Mike to review budget details",
                date: "3 days ago",
                read: false,
            },
        ],
        Starred: [],
        Spam: [
            {
                id: 5,
                sender: "Spammer",
                email: "spam@scam.com",
                subject: "You won $1,000,000!",
                preview: "Click here to claim your prize...",
                body: "CONGRATULATIONS!\n\nYou have been selected to receive $1,000,000 USD!\n\nClick the link below to claim your prize:\nhttp://totally-legit-prize.com/claim\n\nHurry! This offer expires in 24 hours!\n\nSincerely,\nPrize Department",
                date: "1 week ago",
                color: "#FF2D2D",
                read: false,
            },
        ],
        Trash: [],
    },
    "AuthorJohnSmith@gmail.com": {
        Inbox: [
            {
                id: 6,
                sender: "Publisher",
                email: "editor@publishinghouse.com",
                subject: "Book deal",
                preview: "We would like to publish your book...",
                body: "Dear Mr. Smith,\n\nWe are pleased to inform you that we would like to publish your latest manuscript, 'The Quantum Protocol'.\n\nOur editorial team was thoroughly impressed by your work and we believe it has great potential in the current market.\n\nI've attached our initial offer and terms. Please review them at your convenience and let us know if you'd like to discuss further.\n\nLooking forward to working with you.\n\nWarm regards,\nJennifer Adams\nSenior Editor",
                date: "Today",
                color: "#7CFF5B",
                read: false,
                attachments: ["offer_letter.pdf", "terms_conditions.pdf"],
            },
        ],
        Sent: [],
        Drafts: [],
        Starred: [],
        Spam: [],
        Trash: [],
    },
};


//Test Paginacji
const testMails = Array.from({ length: 300 }, (_, i) => ({
    id: i + 1,
    sender: [
        "Laura Shea", "Jonathan Brett", "Conrad Irvin", "Vivek Kumarsafafadfafdafdafdafdafdafdafdafafa",
        "Rahul Vohra", "Sarah Connor", "Mike Johnson", "Emma Wilson",
        "Alex Turner", "Maria Garcia"
    ][i % 10],
    email: `user${i + 1}@example.com`,
    subject: [
        "Closing the dealaaaaaddsafangisghusivgbsuivbgsiuvbgusivbsuivgsiuvgbsivgbsiuvgbsiuvgbsuivgsiuvsiuvuisgbvisgvisuvguis",
        "Start screen & next step",
        "RE: Project update",
        "Meeting tomorrow",
        "Important action needed",
        "Weekly report",
        "Vacation request",
        "Invoice attached",
        "Feedback requested",
        "Newsletter #42"
    ][i % 10],
    preview: `This is a preview of email number ${i + 1}. It shows a snippet of the content...`,
    body: `Full body of email number ${i + 1}.\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit.\n\nBest regards,\nSender`,
    date: `${Math.ceil((i + 1) / 3)} days ago`,
    read: i % 3 === 0,
    color: i % 5 === 0 ? "#7CFF5B" : i % 7 === 0 ? "#FF2D2D" : undefined,
}));

function Inbox() {
    const [currentAccount, setCurrentAccount] = useState(null);
    const [currentFolder, setCurrentFolder] = useState(null);
    const [selectedMail, setSelectedMail] = useState(null);
    const [showCompose, setShowCompose] = useState(false);
    const [replyMail, setReplyMail] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [searchQuery, setSearchQuery] = useState("");
    const [mails, setMails] = useState(testMails);
    const [loading, setLoading] = useState(false);

    /*
    const getCurrentMails = () => {
        if (!currentAccount || !currentFolder) return [];
        return allMails[currentAccount]?.[currentFolder] || []
        //NORMALNE WYŚWIETLANIE
        //allMails[currentAccount]?.[currentFolder] || []
        //DLA TESTU PAGINACJI
        // return testMails
    };
     */

    useEffect(() => {
        loadEmails();
    }, []);

    const loadEmails = async () => {
        setLoading(true);
        try{
            const emails = await mailService.fetchEmails();
            const formattedMails = emails.map(email => ({
                id: email.id,
                sender: email.from || "Unknown",
                email: email.from || "",
                subject: email.subject || "(no subject)",
                preview: (email.content || "").substring(0, 100) + "...",
                body: email.content || "",
                date: formatDate(email.sentDate),
                read: email.read || false,
                attachments: email.attachments || [],
                color: !email.read ? "#7CFF5B" : undefined,
            }));
            setMails(formattedMails);
        }catch (error){
            console.error("Error fetching emails:", error);
        }finally {
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


    const handleFolderClick = (accountId, folderName) => {
        console.log("🟡 handleFolderClick - setting page to 1");
        setCurrentPage(1);
        setSelectedMail(null);
        if (accountId && folderName){
            const account = accounts.find(acc => acc.id === accountId);
            if (account){
                setCurrentAccount(account.email);
                setCurrentFolder(folderName);
            }
        }
    };

    const handleSendMail = () => {
        closeCompose();
    };

    const openCompose = () => {
        setReplyMail(null);
        setShowCompose(true);
    };

    const replyToMail = (mail, type = "reply") => {
        setReplyMail({ ...mail, _type: type });
        setShowCompose(true);
    };

    const closeCompose = () => {
        setShowCompose(false);
        setReplyMail(null);
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
                    />
                ) : (
                    <MailList
                        mails = {currentMails}
                        searchQuery={searchQuery}
                        path= {path}
                        onMailClick={(mail) => setSelectedMail(mail)}
                        currentPage={currentPage}
                        onPageChange={setCurrentPage}
                    />
                )}
            </div>

            {showCompose && (
                <ComposeMail
                    onClose={closeCompose}
                    onSend={handleSendMail}
                    userEmail={currentAccount || "user@quantummail.com"}
                    replyTo={replyMail}
                />
            )}
        </div>
    )
}

export default Inbox;