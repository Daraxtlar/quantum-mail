import "../styles/BadPasswordAlert.css";
import Illegal from "../assets/illegal.png"


function BadPasswordAlert() {
    return (
        <div className={"popup-container"}>
            <div className={"popup-alert"}>
                <div className={"content-wrapper"}>
                    <div className={"icon-holder"}>
                        <img src={Illegal} alt={"x icon"}/>
                    </div>
                    <div className={"desc-holder"}>
                        <p>Invalid password</p>
                    </div>
                </div>
                <div className={"countdown-bar"}></div>
            </div>
        </div>
    );
}

export default BadPasswordAlert