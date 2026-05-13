import React from "react";
import "../styles/Login.css";

function Login(){
    return(
        <div className={"login-container"}>
            <div className={"login-card"}>
                <div className={"login-header"}>

                    <h1>Login to Quantum Mail</h1>
                    <div className={"title-line"}></div>
                </div>

                <form className={"login-form"}>

                    <div className={"input-wrapper"}>
                        <input type={"text"} placeholder={"Login"} className={"login-input"}/>
                        <div className={"password-wrapper"}>
                            <input type={"password"} placeholder={"Password"} className={"login-input"}/>

                            <button type={"button"} className={"reset-btn"}>
                                reset password &gt;&gt;
                            </button>
                        </div>
                    </div>


                    <div className={"submit-wrapper"}>
                        <button type={"submit"} className={"login-btn"}>
                            LOG IN
                        </button>

                        <button type={"button"} className={"register-btn"}>
                            or register &gt;&gt;
                        </button>
                    </div>


                </form>
            </div>
        </div>
    )
}

export default Login;