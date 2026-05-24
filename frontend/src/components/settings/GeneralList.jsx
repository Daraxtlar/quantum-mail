function GeneralList() {
    return (
        <>
            <ul>
                <li className={"setting"}>
                    Draft auto save duration
                </li>
                <li className={"setting"}>
                    Inbox auto refresh duration
                </li>
                <li className={"setting"}>
                    Automatically add incoming email addresses to address book
                </li>
                <li className={"setting"}>
                    Default views
                </li>
                <li className={"setting"}>
                    Automatically view embedded images
                </li>
                <li className={"setting"}>
                    Keep messages in Drafts
                </li>
                <li className={"setting"}>
                    Automatic empty spam/trash duration
                </li>
                <li className={"setting"}>
                    Keyboard shortcuts
                </li>
            </ul>
        </>
    )
}

export default GeneralList