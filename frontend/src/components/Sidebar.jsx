import EmailAccount from "./EmailAccount.jsx";
import "../styles/Sidebar.css";
import {useState} from "react";

//Przykładowe dane do wyświetlenia później do usunięcia
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

function Sidebar() {
    const [expandedAccount, setExpandedAccount] = useState(null);
    const [activeFolder, setActiveFolder] = useState(null);

    const handleAccountClick = (accountId) => {
        setExpandedAccount(expandedAccount === accountId ? null : accountId);
    };

    const handleFolderClick = (accountId, folderName) => {
        setActiveFolder({accountId, folderName});
    }


    return (
        <aside className={"sidebar"}>
            <div className={"accounts-wrapper"}>
                {accounts.map((account) => (
                    <EmailAccount
                        key={account.id}
                        account={account}
                        isOpen={expandedAccount === account.id}
                        onToggle={() => handleAccountClick(account.id)}
                        activeFolder={activeFolder}
                        onFolderClick={handleFolderClick}
                    />
                ))}
            </div>
        </aside>
    );
}

export default Sidebar;