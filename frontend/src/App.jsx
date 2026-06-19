import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx"
import Inbox from "./pages/Inbox.jsx"
import Settings from "./pages/Settings.jsx"
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import ProtectedRoute from "./components/ProtectedRoute.jsx";

function App() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Login/>}/>
                <Route path="/login" element={<Login/>}/>
                <Route path="/register" element={<Register/>}/>
                <Route element={<ProtectedRoute/>}>
                    <Route path="/inbox" element={<Inbox/>}/>
                    <Route path="/settings" element={<Settings/>}/>
                </Route>

                <Route path="*" element={<Navigate to="/inbox" replace />} />
            </Routes>
        </BrowserRouter>
    )
}

export default App
