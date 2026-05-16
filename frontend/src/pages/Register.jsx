import "react";
import {Link} from 'react-router-dom';
import "../styles/Login.css";

function Register() {
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

                <form className={"login-form"}>

                    <div className={"input-wrapper"}>
                        <input type={"text"} placeholder={"Username"} className={"login-input"}/>
                        <div className={"password-wrapper"}>
                            <input type={"password"} placeholder={"Password"} className={"login-input"}/>
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