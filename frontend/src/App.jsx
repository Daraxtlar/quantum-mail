import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx"
import Inbox from "./pages/Inbox.jsx"
import Settings from "./pages/Settings.jsx"
import {BrowserRouter, Route, Routes} from 'react-router-dom';

function App() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Login/>}/>
                <Route path="/inbox" element={<Inbox/>}/>
                <Route path="/login" element={<Login/>}/>
                <Route path="/register" element={<Register/>}/>
                <Route path="/settings" element={<Settings/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App
