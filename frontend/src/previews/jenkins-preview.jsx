import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import JenkinsDesignPreview from './JenkinsDesignPreview.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <JenkinsDesignPreview />
    </StrictMode>
)
