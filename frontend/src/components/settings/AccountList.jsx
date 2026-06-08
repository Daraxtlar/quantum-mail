import "../../styles/Settings.css";
import {useState} from "react";
import {AccountService} from "../../services/AccountService.js";
import {useNavigate} from "react-router-dom";

function AccountList() {
    const navigate = useNavigate();
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [deletePassword, setDeletePassword] = useState('');
    const [message, setMessage] = useState('');

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        try {
            const result = await AccountService.changePassword(oldPassword, newPassword);
            setMessage("Password changed successfully!" + result);
            setOldPassword('');
            setNewPassword('');
        } catch (err) {
            setMessage("Error changing password: " + err.message);
        }
    }

    const handleDeleteAccount = async () => {
        if (!deletePassword) {
            setMessage("Please enter your password to confirm account deletion.");
            return;
        }

        if (!window.confirm("Are you sure you want to delete your account? This action cannot be undone.")) {
            return;
        }

        try {
            await AccountService.deleteAccount(deletePassword);
            localStorage.removeItem('token');
            navigate("/login");
        } catch (err) {
            setMessage("Error deleting account: " + err.message);
        }
    }

    return (
        <div className={"account-settings"}>
            {message && <div className={"alert"}>{message}</div>}

            <section className={"settings-section"}>
                <h3> Change Password </h3>
                <form onSubmit={handlePasswordChange}>
                    <input type={"password"} placeholder={"Old Password"} value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required/>
                    <input type={"password"} placeholder={"New Password"} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required/>
                    <button type={"submit"}>Change Password</button>
                </form>
            </section>

            <section className={"settings-section delete-account"}>
                <h3> Delete Account </h3>
                <input type={"password"} placeholder={"Enter your password to confirm"} value={deletePassword} onChange={(e) => setDeletePassword(e.target.value)}/>
                <button onClick={handleDeleteAccount} className={"delete-btn"}>Delete Account</button>
            </section>
        </div>
    )
}

export default AccountList