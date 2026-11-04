import { Link } from 'react-router-dom';

export function ProjectList({ projects, selectedId }) {
  if (projects.length === 0) {
    return <p className="empty-search">Aramanızla eşleşen proje bulunamadı.</p>;
  }

  return (
    <div className="project-list" aria-label="Kayıtlı projeler">
      {projects.map((project) => (
        <Link
          key={project.id}
          to={`/projects/${project.id}`}
          className={selectedId === project.id ? 'project-row selected' : 'project-row'}
          aria-current={selectedId === project.id ? 'page' : undefined}
        >
          <span className="project-avatar" aria-hidden="true">
            {project.name.slice(0, 2).toUpperCase()}
          </span>
          <span className="project-title">
            <strong>{project.name}</strong>
            <small>{new URL(project.repositoryUrl).host}</small>
          </span>
          <span className="environment">{project.environment}</span>
          <span className={project.enabled ? 'status enabled' : 'status neutral'}>
            <span className="status-dot" />
            {project.enabled ? 'Aktif profil' : 'Pasif profil'}
          </span>
          <span className="row-arrow" aria-hidden="true">
            ↗
          </span>
        </Link>
      ))}
    </div>
  );
}
