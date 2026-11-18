import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import ProjectDetails from '../components/projects/ProjectDetails.jsx'
import ProjectForm from '../components/projects/ProjectForm.jsx'
import { projectService } from '../services/projectService.js'

function ProjectDetailsContent({ projectId }) {
    const navigate = useNavigate()
    const location = useLocation()
    const [project, setProject] = useState(null)
    const [loadError, setLoadError] = useState(null)
    const [loadAttempt, setLoadAttempt] = useState(0)
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState('')
    const [notice, setNotice] = useState(location.state?.notice ?? '')
    const activeRequest = useRef(null)

    useEffect(() => {
        const controller = new AbortController()
        activeRequest.current = controller

        async function loadProject() {
            try {
                const result = await projectService.getProjectById(projectId, {
                    signal: controller.signal
                })
                if (!controller.signal.aborted) {
                    setProject(result)
                }
            } catch (failure) {
                if (!controller.signal.aborted) {
                    setLoadError(failure)
                }
            }
        }

        loadProject()
        return () => controller.abort()
    }, [projectId, loadAttempt])

    function handleRetry() {
        setLoadError(null)
        setLoadAttempt((attempt) => attempt + 1)
    }

    async function handleSave(request) {
        const savedProject = await projectService.updateProject(projectId, request)
        setProject(savedProject)
        setIsFormOpen(false)
        setDeleteError('')
        setNotice('Proje profili kaydedildi.')
    }

    async function handleDelete() {
        if (isDeleting || !window.confirm(`“${project.name}” profilini silmek istiyor musunuz?`)) {
            return
        }

        const signal = activeRequest.current.signal
        setIsDeleting(true)
        setDeleteError('')
        setNotice('')
        try {
            await projectService.deleteProject(projectId)
            if (!signal.aborted) {
                navigate('/projects', {
                    replace: true,
                    state: { notice: 'Proje profili silindi.' }
                })
            }
        } catch (failure) {
            setDeleteError(failure.message)
        } finally {
            setIsDeleting(false)
        }
    }

    function renderProject() {
        if (project) {
            return (
                <ProjectDetails
                    project={project}
                    deleting={isDeleting}
                    onEdit={() => setIsFormOpen(true)}
                    onDelete={handleDelete}
                />
            )
        }
        if (loadError?.status === 404 || loadError?.status === 400) {
            return (
                <p className="notice error" role="alert">
                    Proje bulunamadı.
                </p>
            )
        }
        if (loadError) {
            return (
                <>
                    <p className="notice error" role="alert">
                        {loadError.message}
                    </p>
                    <button className="button secondary" onClick={handleRetry}>
                        Tekrar dene
                    </button>
                </>
            )
        }
        return (
            <p className="empty-state" role="status">
                Proje yükleniyor…
            </p>
        )
    }

    return (
        <div className="workspace-content">
            <Link className="project-back-link" to="/projects">
                <span aria-hidden="true">←</span> Proje listesine dön
            </Link>
            {notice && (
                <p className="notice success" role="status">
                    {notice}
                </p>
            )}
            {deleteError && (
                <p className="notice error" role="alert">
                    {deleteError}
                </p>
            )}
            {renderProject()}
            {isFormOpen && (
                <ProjectForm
                    project={project}
                    onSave={handleSave}
                    onClose={() => setIsFormOpen(false)}
                />
            )}
        </div>
    )
}

function ProjectDetailsPage() {
    const { projectId } = useParams()
    return <ProjectDetailsContent key={projectId} projectId={projectId} />
}

export default ProjectDetailsPage
