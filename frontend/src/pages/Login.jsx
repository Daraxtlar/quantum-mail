import "react";
import {Link} from 'react-router-dom';
import "../styles/Login.css";
import {useState} from "react";
import Modial from "../components/ResetPasswordPopup.jsx"
import { useNavigate } from "react-router-dom";
import Alert from "../components/BadPasswordAlert.jsx";

function Login() {
    const [showModal, setShowModal] = useState(false)
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const navigate = useNavigate();
    const [showAlert, setShowAlert] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()

        const response = await fetch("http://localhost:8080/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();

            localStorage.setItem("user", JSON.stringify(data));

            navigate("/inbox");
        }
        else{
            setShowAlert(true);
        }
    }

    return (
        <div className={"login-container"}>

            <div className={"alert-holder"}>
                {
                    showAlert && <Alert />
                }
            </div>
            <div className={"login-card"}>
                <div className={"login-header"}>

                    <h1>Sign in to Quantum Mail</h1>
                    <div className={"title-line"}></div>
                </div>

                <form onSubmit={handleSubmit} className={"login-form"}>

                    <div className={"input-wrapper"}>
                        <input value={username}
                               type={"text"}
                               placeholder={"Login"}
                               className={"login-input"}
                               onChange={(e) => setUsername(e.target.value)}/>
                        <div className={"password-wrapper"}>
                            <input value={password}
                                   type={"password"}
                                   placeholder={"Password"}
                                   className={"login-input"}
                                   onChange={(e) => setPassword(e.target.value)}/>

                            <button type={"button"} onClick={() => setShowModal(true)} className={"reset-btn"}>
                                reset password »
                            </button>
                            {
                                showModal && <Modial/>
                            }
                        </div>
                    </div>


                    <div className={"submit-wrapper"}>
                        <button type={"submit"} className={"login-btn"}>
                            SIGN IN
                        </button>

                        <Link to="/register">
                            <button type={"button"} className={"register-btn"}>
                                or register »
                            </button>
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default Login;