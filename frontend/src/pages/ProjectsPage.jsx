import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import ProjectForm from '../components/projects/ProjectForm.jsx'
import ProjectList from '../components/projects/ProjectList.jsx'
import { useProjects } from '../hooks/useProjects.js'
import { projectService } from '../services/projectService.js'

function ProjectsPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const { projects, isLoading, error, reload } = useProjects()
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [search, setSearch] = useState('')

    const searchText = search.trim().toLocaleLowerCase('tr-TR')
    const filteredProjects = projects.filter((project) =>
        project.name.toLocaleLowerCase('tr-TR').includes(searchText)
    )

    async function handleSave(request) {
        const project = await projectService.saveProject(request)
        navigate(`/projects/${project.id}`, {
            state: { notice: 'Proje profili kaydedildi.' }
        })
    }

    function renderProjectList() {
        if (projects.length > 0) {
            return <ProjectList projects={filteredProjects} />
        }
        if (isLoading) {
            return (
                <p className="empty-state" role="status">
                    Projeler yükleniyor…
                </p>
            )
        }
        if (!error) {
            return <p className="empty-state">Henüz izlenen proje yok.</p>
        }
        return null
    }

    return (
        <div className="workspace-content projects-page">
            <header className="page-heading">
                <h1>Projeler</h1>
                <button className="button primary" onClick={() => setIsFormOpen(true)}>
                    Yeni proje
                </button>
            </header>
            {location.state?.notice && (
                <p className="notice success" role="status">
                    {location.state.notice}
                </p>
            )}
            {error && (
                <p className="notice error" role="alert">
                    {error}
                </p>
            )}
            <div className="projects-toolbar">
                <label className="search-field">
                    <span className="sr-only">Proje adına göre ara</span>
                    <input
                        type="search"
                        placeholder="Proje adına göre ara…"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                    />
                </label>
                <button className="button secondary" disabled={isLoading} onClick={reload}>
                    {isLoading ? 'Yükleniyor…' : 'Yenile'}
                </button>
            </div>
            {renderProjectList()}
            {isFormOpen && <ProjectForm onSave={handleSave} onClose={() => setIsFormOpen(false)} />}
        </div>
    )
}

export default ProjectsPage
