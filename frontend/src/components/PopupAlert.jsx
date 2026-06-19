import "../styles/PopupAlert.css";
import Illegal from "../assets/illegal.png";
import { XCircle, CheckCircle2, Info} from "lucide-react";
import {useEffect} from "react";


function PopupAlert({ message, type="success", onClose}) {

    useEffect(() => {
        if (!onClose) return;

        const timer = setTimeout(() => {
            onClose();
        }, 2000);

        return () => clearTimeout(timer);
    }, [onClose]);

    const renderIcon = () => {
        switch (type){
            case "error":
                return <XCircle size={22} className={"alert-icon error"} />;
            case "success":
                return <CheckCircle2 size={22} className={"alert-icon success"} />;
            default:
                return <Info size={22} className={"alert-icon info"} />;
        }
    }

    return (
        <div className={"popup-container"}>
            <div className={`popup-alert ${type}`}>
                <div className={"content-wrapper"}>
                    <div className={"icon-holder"}>
                        {renderIcon()}
                    </div>
                    <div className={"desc-holder"}>
                        <p>{message}</p>
                    </div>
                </div>
                <div className={"countdown-bar"}></div>
            </div>
        </div>
    );
}

export default PopupAlert;