import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import JenkinsDetailsPage from '../pages/JenkinsDetailsPage.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <JenkinsDetailsPage />
    </StrictMode>
)
