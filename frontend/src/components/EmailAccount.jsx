import {ChevronDown, ChevronRight} from "lucide-react";
import SidebarItem from "./SidebarItem.jsx";
import "../styles/Sidebar.css";

function EmailAccount({account, isOpen, onToggle, activeFolder, onFolderClick}) {
    const hasFolders = account.folders.length > 0;

    const totalUnread = account.folders.reduce((sum, folder) => sum + (folder.unread || 0), 0);

    return (
        <div className={"email-account"}>
            <div className={`account-header ${isOpen ? "expanded" : ""}`}
                 onClick={onToggle} style={{cursor: hasFolders ? "pointer" : "default"}}>

                {hasFolders ? (
                    isOpen ? <ChevronDown size={16}/> : <ChevronRight size={16}/>
                ) : (
                    <ChevronRight size={16} opacity={0.3}/>
                )}

                <span className={"account-email"}>{account.email}</span>

                {totalUnread > 0 && (
                    <div className={"badge"}>{totalUnread}</div>
                )}
            </div>

            {hasFolders && isOpen && (
                <div className={"folder-list"}>
                    {account.folders.map((folder, index) => {
                        const isActive = activeFolder?.accountId === account.id &&
                            activeFolder?.folderName === folder.name;

                        return (
                            <SidebarItem
                                key={index}
                                item={{...folder, active: isActive}}
                                onClick={() => onFolderClick(account.id, folder.name)}
                            />
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default EmailAccount;