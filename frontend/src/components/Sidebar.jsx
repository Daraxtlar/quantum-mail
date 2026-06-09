import EmailAccount from "./EmailAccount.jsx";
import "../styles/Sidebar.css";
import AddAddressModal from "./AddAccountWizard.jsx";
import {useState} from "react";
import AddAccountWizard from "./AddAccountWizard.jsx";


function Sidebar({accounts= [], onFolderClick}) {
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


    return (
        <aside className={"sidebar"}>
            <button onClick={() => setShowAddModal(true)}>
                Add Account
            </button>

            {showAddModal && (
                <AddAccountWizard
                    onClose={() => setShowAddModal(false)}
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

                {
                    showAddModal && <AddAddressModal/>
                }
            </div>
        </aside>
    );
}

export default Sidebar;