import "react";
import "../styles/Settings.css"
import "../styles/fontello.css"
import AppearanceList from "../components/settings/AppearanceList.jsx";
import {useState} from "react";
import IntegrationsList from "../components/settings/IntegrationsList.jsx";
import AccountList from "../components/settings/AccountList.jsx";
import {Link} from "react-router-dom";

function Settings() {
    const [settingList, setSettingList] = useState("appearance")
    return (
        <div className={"main-container"}>
            <div className={"settings-wrapper"}>
                <div className={"nav-options"}>
                    <ul>
                        <li className={"option"} id={"appearance"} onClick={() => setSettingList("appearance")}>
                            <i className={"icon-brush"}></i>
                            Appearance
                        </li>
                        <li className={"option"} id={"integrations"} onClick={() => setSettingList("integrations")}>
                            <i className={"icon-flow-merge"}></i>
                            Integrations
                        </li>
                        <li className={"option"} id={"account"} onClick={() => setSettingList("account")}>
                            <i className={"icon-user"}></i>
                            Account
                        </li>
                    </ul>
                </div>

                <div className={"options"}>
                    <div className={"options-header"}>
                        <Link to="/inbox">
                            <div className={"back-button"}>
                                <i className={"icon-left-small"}></i>
                            </div>
                        </Link>
                        <div className={"title-settings"}>
                            <h1>{settingList}</h1>
                        </div>
                        <div className={"search-box"}>
                            <i className={"icon-search"}></i>
                            <input
                                placeholder={"Search"} />
                        </div>
                    </div>
                    {
                        settingList === "appearance" && <AppearanceList/>
                    }
                    {
                        settingList === "integrations" && <IntegrationsList/>
                    }
                    {
                        settingList === "account" && <AccountList/>
                    }
                </div>
            </div>
        </div>

    )
}

export default Settings;