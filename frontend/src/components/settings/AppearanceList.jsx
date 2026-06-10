import {useEffect, useState} from "react";
import "../../styles/Settings.css";

function AppearanceList() {
    const [theme, setTheme] = useState(localStorage.getItem("theme") || "default");
    const [font, setFont] = useState(localStorage.getItem("font-family") || "Arial, Helvetica, sans-serif");

    useEffect(() => {
        document.documentElement.setAttribute("data-theme", theme);
        localStorage.setItem("theme", theme);
    }, [theme]);

    useEffect(() => {
        document.documentElement.style.setProperty("--font-family", font);
        localStorage.setItem("font-family", font);
    }, [font]);

    return (
        <div className={"appearance-settings-container"}>
            <div className={"setting-group"}>
                <label className={"setting-label"}>App Theme</label>
                <select
                    className={"setting-select"}
                    value={theme}
                    onChange={(e) => setTheme(e.target.value)}
                >
                    <option value={"sunset-dark"}>Sunset</option>
                    <option value={"midnight"}>Midnight</option>
                    <option value={"emerald-dark"}>Emerald</option>
                    <option value={"carbon"}>Carbon</option>
                    <option value={"light"}>Light</option>
                    <option value={"default"}>Default</option>
                </select>
            </div>

            <div className={"setting-group"}>
                <label className={"setting-label"}>Font Type</label>
                <select
                    className={"setting-select"}
                    value={font}
                    onChange={(e) => setFont(e.target.value)}
                >
                    <option value={"Arial, Helvetica, sans-serif"}>Sans-serif</option>
                    <option value={"'Inter', sans-serif"}>Inter</option>
                    <option value={"'Courier New', Courier, monospace"}>Monospace</option>
                    <option value={"Georgia, 'Times New Roman', serif"}>Serif</option>
                </select>
            </div>
        </div>
    )
}

export default AppearanceList