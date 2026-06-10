import "react";
import {Link, useNavigate} from 'react-router-dom';
import "../styles/Login.css";
import {useState} from "react";
import {authService} from "../services/AuthService.js";

function Register() {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [email, setEmail] = useState('')
    const navigate = useNavigate();

    async function handleSubmit(e) {
        e.preventDefault()

        try {
            await authService.register(username, password, email);
            navigate("/login");
        }catch (err) {
            //TODO Jakiś error normalny do wyświetlenia
            console.error("Błąd rejestracji:", err);
        }
    }

    return (
        <div className={"login-container"}>
            <div className={"login-card"}>
                <div className={"login-header"}>

                    <h1>Register to Quantum Mail</h1>
                    <div className={"title-line"}></div>
                </div>

                <div className={"login-link"}>
                    Already have an account?
                    <Link to="/login">

                        <button type={"button"} className={"register-btn"}>
                            Sign in »
                        </button>
                    </Link>
                </div>

                <form onSubmit={handleSubmit} className={"login-form"}>

                    <div className={"input-wrapper"}>
                        <input
                            value={email}
                            type={"text"}
                            placeholder={"Email"}
                            className={"login-input"}
                            onChange={(e) => setEmail(e.target.value)}/>
                        <input
                            value={username}
                            type={"text"}
                            placeholder={"Username"}
                            className={"login-input"}
                            onChange={(e) => setUsername(e.target.value)}/>
                        <div className={"password-wrapper"}>
                            <input
                                value={password}
                                type={"password"}
                                placeholder={"Password"}
                                className={"login-input"}
                                onChange={(e) => setPassword(e.target.value)}/>
                        </div>
                    </div>

                    <p className={"tos-notice"}>
                        By creating an account, you agree to the Terms of Service.
                    </p>


                    <div className={"submit-wrapper"}>
                        <button type={"submit"} className={"login-btn"}>
                            CREATE AN ACCOUNT
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default Register;