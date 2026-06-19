import { useState } from "react";
import { X, Mail, Settings } from "lucide-react";

import "../styles/AddAccountWizard.css";
import {EmailAccountService} from "../services/EmailAccountService.js";

function AddAccountWizard({ onClose, onAccountAdded }) {
    const [step, setStep] = useState(1);

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [imapHost, setImapHost] = useState("");
    const [imapPort, setImapPort] = useState("993");
    const [ssl, setSsl] = useState(true);

    const [smtpHost, setSmtpHost] = useState("");
    const [smtpPort, setSmtpPort] = useState("465");
    const [smtpSsl, setSmtpSsl] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    async function handleManualSubmit(e) {
        e.preventDefault();
        setErrorMessage("");
        setStep(3);

        const payload = {
            emailAddress: email,
            password: password,
            imapHost: imapHost,
            imapPort: parseInt(imapPort, 10),
            sslEnabled: ssl,
            smtpHost: smtpHost,
            smtpPort: parseInt(smtpPort, 10),
            smtpSslEnabled: smtpSsl
        };

        try {
            await EmailAccountService.addAccount(payload);
            setStep(4);
        } catch (error) {
            setStep(2);
            const apiError = error.response?.data?.message
                || error.response?.data?.error
                || "Could not connect to mail server. Verify your settings.";
            setErrorMessage(apiError);
        }
    }

    return (
        <div className="popup-overlay">
            <div className="wizard-modal">

                <button
                    type="button"
                    className="close-button"
                    onClick={() => {
                        console.log("X clicked");
                        onClose();
                    }}
                >
                    <X size={24} />
                </button>

                <h2>Add Email Account</h2>

                <div className="wizard-progress">
                    <div className={`wizard-step ${step >= 1 ? "active" : ""}`}>
                        <div className="step-circle">1</div>
                        <span>Provider</span>
                    </div>

                    <div className={`wizard-line ${step >= 2 ? "active" : ""}`} />

                    <div className={`wizard-step ${step >= 2 ? "active" : ""}`}>
                        <div className="step-circle">2</div>
                        <span>Setup</span>
                    </div>

                    <div className={`wizard-line ${step >= 3 ? "active" : ""}`} />

                    <div className={`wizard-step ${step >= 3 ? "active" : ""}`}>
                        <div className="step-circle">3</div>
                        <span>Verify</span>
                    </div>
                </div>

                {step === 1 && (
                    <div className="provider-grid">

                        <div
                            className="provider-card"
                            onClick={() => {
                                window.location.href =
                                    "http://localhost:8080/oauth/google/login";
                            }}
                        >
                            <Mail size={42} />
                            <h3>Gmail</h3>
                            <p>Connect using Google OAuth.</p>
                        </div>

                        <div
                            className="provider-card"
                            onClick={() => setStep(2)}
                        >
                            <Settings size={42} />
                            <h3>Manual Setup</h3>
                            <p>Configure IMAP settings manually.</p>
                        </div>

                    </div>
                )}

                {step === 2 && (
                    <form
                        className="wizard-form"
                        onSubmit={handleManualSubmit}
                    >
                        <input
                            className="wizard-input"
                            type="email"
                            placeholder="Email address"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />

                        <input
                            className="wizard-input"
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />

                        <div className="settings-section">
                            <h3>Incoming Mail (IMAP)</h3>

                            <input
                                className="wizard-input"
                                type="text"
                                placeholder="imap.example.com"
                                value={imapHost}
                                onChange={(e) =>
                                    setImapHost(e.target.value)
                                }
                                required
                            />

                            <input
                                className="wizard-input"
                                type="number"
                                placeholder="993"
                                value={imapPort}
                                onChange={(e) =>
                                    setImapPort(e.target.value)
                                }
                                required
                            />

                            <label className="checkbox-row">
                                <input
                                    type="checkbox"
                                    checked={ssl}
                                    onChange={(e) =>
                                        setSsl(e.target.checked)
                                    }
                                />
                                Use SSL/TLS
                            </label>
                        </div>

                        <div className="settings-section">
                            <h3>Outgoing Mail (SMTP)</h3>

                            <input
                                className="wizard-input"
                                type="text"
                                placeholder="smtp.example.com"
                                value={smtpHost}
                                onChange={(e) =>
                                    setSmtpHost(e.target.value)
                                }
                                required
                            />

                            <input
                                className="wizard-input"
                                type="number"
                                placeholder="465"
                                value={smtpPort}
                                onChange={(e) =>
                                    setSmtpPort(e.target.value)
                                }
                                required
                            />

                            <label className="checkbox-row">
                                <input
                                    type="checkbox"
                                    checked={smtpSsl}
                                    onChange={(e) =>
                                        setSmtpSsl(e.target.checked)
                                    }
                                />
                                Use SSL/TLS
                            </label>
                        </div>

                        {errorMessage && (
                            <div className={"error-message"}>
                                {errorMessage}
                            </div>
                        )}

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={() => setStep(1)}
                            >
                                Back
                            </button>

                            <button
                                type="submit"
                                className="primary-button"
                            >
                                Verify Connection
                            </button>
                        </div>
                    </form>
                )}

                {step === 3 && (
                    <div className="verification-screen">
                        <div className="spinner" />
                        <h3>Verifying connection...</h3>
                        <p>
                            Testing your mailbox settings and credentials.
                        </p>
                    </div>
                )}

                {step === 4 && (
                    <div className="success-screen">
                        <div className="success-icon">✓</div>
                        <h3>Account Added</h3>
                        <p>Your mailbox has been connected.</p>

                        <button
                            className="primary-button"
                            onClick={() =>{
                                if (onAccountAdded) onAccountAdded()
                                onClose()
                            }}
                        >
                            Finish
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

export default AddAccountWizard;