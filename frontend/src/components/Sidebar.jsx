import EmailAccount from "./EmailAccount.jsx";
import "../styles/Sidebar.css";
import {useState} from "react";


function Sidebar({accounts= [], onFolderClick}) {
    const [expandedAccount, setExpandedAccount] = useState(null);
    const [activeFolder, setActiveFolder] = useState(null);

    const handleAccountClick = (accountId) => {
        setExpandedAccount(expandedAccount === accountId ? null : accountId);
    };

    const handleFolderClick = (accountId, folderName) => {
        setActiveFolder({accountId, folderName});
        if (onFolderClick){
            onFolderClick(accountId, folderName);
        }
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