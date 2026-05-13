import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'
import './App.css'
import Login from "./pages/Login.jsx";
import Home from "./pages/Home.jsx";

function App() {
  const [count, setCount] = useState(0)

  return (
    <Home />
  )
}

export default App
