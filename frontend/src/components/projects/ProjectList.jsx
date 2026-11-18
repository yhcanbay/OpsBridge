import { Link } from 'react-router-dom'

function ProjectList({ projects }) {
    if (projects.length === 0) {
        return <p className="empty-search">Aramanızla eşleşen proje bulunamadı.</p>
    }

    return (
        <ul className="project-list" aria-label="İzlenen projeler">
            {projects.map((project) => (
                <li key={project.id}>
                    <Link to={`/projects/${project.id}`} className="project-row">
                        <span>{project.name}</span>
                        <span className="row-arrow" aria-hidden="true">
                            →
                        </span>
                    </Link>
                </li>
            ))}
        </ul>
    )
}

export default ProjectList
