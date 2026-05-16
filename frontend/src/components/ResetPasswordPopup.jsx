import "../styles/ResetPasswordPopup.css";
import {X} from "lucide-react";

function ResetPasswordPopup() {
    return (
        <div className="popup-overlay">
            <div className="popup-container">
                <button className="close-button">
                    <X size={30}/>
                </button>

                <div className="popup-card">
                    <h2>Reset password</h2>

                    <p>
                        Enter your login below. If you've chosen a recovery option, it should send you further
                        instructions to recover your account.
                    </p>

                    <input
                        type="text"
                        placeholder="login"
                        className="popup-input"
                    />

                    <button className="reset-button">
                        Reset password
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ResetPasswordPopup