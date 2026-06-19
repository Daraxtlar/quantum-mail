import "../../styles/Settings.css"
import {useEffect, useState} from "react";
import {EmailAccountService} from "../../services/EmailAccountService.js";
import {mailService} from "../../services/MailService.js";

function IntegrationsList() {
    const [accounts, setAccounts] = useState([]);
    const [selectedAccount, setSelectedAccount] = useState("");
    const [suggestions, setSuggestions] = useState([]);
    const [newRecipient, setNewRecipient] = useState("");

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect( () => {
        loadAccounts();
    }, []);

    useEffect(() => {
        if (selectedAccount){
            loadSuggestions(selectedAccount);
        }else {
            setSuggestions([]);
        }
    }, [selectedAccount]);

    const loadAccounts = async () => {
        try {
            const data = await EmailAccountService.fetchAccounts();
            setAccounts(data);
            if (data.length > 0) {
                setSelectedAccount(data[0].emailAddress);
            }
        } catch (err) {
            setError("Error fetching email accounts: " + err.message);
        }
    };

    const loadSuggestions = async (email) => {
        try{
            const data = await mailService.fetchSuggestions(email);
            setSuggestions(data);
        }catch (err) {
            setError("Error fetching suggestions: " + err.message);
        }
    };

    const handleDeleteAccount = async (emailAddress) => {
        if (!window.confirm(`Are you sure you want to delete the account ${emailAddress}? This action cannot be undone.`)) {
            return;
        }

        try{
            setMessage("Deleting account...");
            setError("");

            await EmailAccountService.deleteAccount(emailAddress);
            setMessage("Account deleted successfully.");
            loadAccounts();
        } catch (err) {
            setError("Error deleting account: " + err.message);
            setMessage("");
        }
    }

    const handleAddSuggestion = async (e) => {
        e.preventDefault();

        if (!selectedAccount){
            setError("Please select an email account first.");
            return;
        }

        if (!newRecipient.trim()) return;

        try{
            setMessage("Adding suggestion...");
            setError("");

            await mailService.addSuggestion(selectedAccount, newRecipient.trim());
            setMessage("Suggestion added successfully.");
            setNewRecipient("");
            loadSuggestions(selectedAccount);
        } catch (err) {
            setError("Error adding suggestion: " + err.message);
            setMessage("");
        }
    };

    const handleDeleteSuggestion = async (recipientEmail) => {
        if (!window.confirm(`Are you sure you want to delete the suggestion for ${recipientEmail}? This action cannot be undone.`)) {
            return;
        }

        try{
            setMessage("Deleting suggestion...");
            setError("");

            await mailService.deleteSuggestion(selectedAccount, recipientEmail);
            setMessage("Suggestion deleted successfully.");
            loadSuggestions(selectedAccount);
        }catch (err) {
            setError("Error deleting suggestion: " + err.message);
            setMessage("");
        }
    };

    return (
        <div className={"account-settings"}>
            {message && <div className={"alert"}>{message}</div>}
            {error && <div className={"alert alert-error"}>{error}</div>}

            <section className={"settings-section"}>
                <h3> Connected Email Accounts </h3>
                {accounts.length === 0 ? (
                    <p className={"accounts-empty"}>No email accounts connected. Please add an account to see it here.</p>
                ) : (
                    <div className={"connected-accounts-list"}>
                        {accounts.map((account) => (
                            <div key={account.id} className={"account-item"}>
                                <span className={"account-email-text"}>{account.emailAddress}</span>
                                <button
                                    onClick={() => handleDeleteAccount(account.emailAddress)} className={"delete-button delete-btn"}>
                                    Delete
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            <section className="settings-section delete-account">
                <h3> Address Book Suggestions </h3>

                <div className="setting-group" style={{ maxWidth: "100%" }}>
                    <label className="setting-label">Manage suggestions for identity:</label>
                    <select
                        className="setting-select"
                        value={selectedAccount}
                        onChange={(e) => setSelectedAccount(e.target.value)}
                    >
                        {accounts.length === 0 && <option value="">No accounts available</option>}
                        {accounts.map(acc => (
                            <option key={acc.id} value={acc.emailAddress}>{acc.emailAddress}</option>
                        ))}
                    </select>
                </div>

                <form onSubmit={handleAddSuggestion} className="add-suggestion-form">
                    <input
                        type="email"
                        placeholder="Enter recipient email..."
                        value={newRecipient}
                        onChange={(e) => setNewRecipient(e.target.value)}
                        required
                    />
                    <button type="submit">Add Contact</button>
                </form>

                <div className="scrollable-contacts-container">
                    {suggestions.length === 0 ? (
                        <p className="accounts-empty">No suggestions found for this account.</p>
                    ) : (
                        <div className="connected-accounts-list">
                            {suggestions.map((recipientEmail, index) => (
                                <div key={index} className="account-item suggestion-item">
                                    <span className="account-email-text">{recipientEmail}</span>
                                    <button
                                        onClick={() => handleDeleteSuggestion(recipientEmail)}
                                        className="delete-button delete-btn"
                                    >
                                        Remove
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </section>

        </div>
    )
}

export default IntegrationsList