import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { AuthProvider } from './shared/auth/AuthContext'
import { App } from './app/router'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
  <AuthProvider>
    <App />
  </AuthProvider>
</StrictMode>
)
