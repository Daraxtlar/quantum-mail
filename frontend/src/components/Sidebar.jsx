import EmailAccount from "./EmailAccount.jsx";
import "../styles/Sidebar.css";
import {useState} from "react";
import AddAccountWizard from "./AddAccountWizard.jsx";


function Sidebar({accounts= [], onFolderClick, onAccountAdded}) {
    const [expandedAccount, setExpandedAccount] = useState(null);
    const [activeFolder, setActiveFolder] = useState(null);
    const [showAddModal, setShowAddModal] = useState(false)

    const handleAccountClick = (accountId) => {
        setExpandedAccount(expandedAccount === accountId ? null : accountId);
    };

    const handleFolderClick = (accountId, folderName) => {
        setActiveFolder({accountId, folderName});
        if (onFolderClick){
            onFolderClick(accountId, folderName);
        }
    }

    const closeModal = () =>{
        setShowAddModal(false);
    }


    return (
        <aside className={"sidebar"}>
            <button onClick={() => setShowAddModal(true)}>
                Add Account
            </button>

            {showAddModal && (
                <AddAccountWizard
                    onClose={closeModal}
                    onAccountAdded={onAccountAdded}
                />
            )}
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