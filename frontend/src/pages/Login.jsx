import "react";
import {Link} from 'react-router-dom';
import "../styles/Login.css";
import {useState} from "react";
import Modial from "../components/ResetPasswordPopup.jsx"

function Login() {
    const [showModial, setShowModial] = useState(false)
    return (
        <div className={"login-container"}>
            <div className={"login-card"}>
                <div className={"login-header"}>

                    <h1>Sign in to Quantum Mail</h1>
                    <div className={"title-line"}></div>
                </div>

                <form className={"login-form"}>

                    <div className={"input-wrapper"}>
                        <input type={"text"} placeholder={"Login"} className={"login-input"}/>
                        <div className={"password-wrapper"}>
                            <input type={"password"} placeholder={"Password"} className={"login-input"}/>

                            <button type={"button"} onClick={() => setShowModial(true)} className={"reset-btn"}>
                                reset password »
                            </button>
                            {
                                showModial && <Modial/>
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