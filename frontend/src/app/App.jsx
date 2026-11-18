import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../components/auth/ProtectedRoute.jsx'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import LoginPage from '../pages/LoginPage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import ProjectsPage from '../pages/ProjectsPage.jsx'
import ProjectDetailsPage from '../pages/ProjectDetailsPage.jsx'
import JenkinsDetailsPage from '../pages/JenkinsDetailsPage.jsx'
import DockerDetailsPage from '../pages/DockerDetailsPage.jsx'

function App() {
    return (
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
                <Route element={<DashboardLayout />}>
                    <Route path="/" element={<Navigate to="/projects" replace />} />
                    <Route path="/projects" element={<ProjectsPage />} />
                    <Route path="/projects/:projectId" element={<ProjectDetailsPage />} />
                    <Route path="/projects/:projectId/jenkins" element={<JenkinsDetailsPage />} />
                    <Route path="/projects/:projectId/docker" element={<DockerDetailsPage />} />
                </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    )
}

export default App
