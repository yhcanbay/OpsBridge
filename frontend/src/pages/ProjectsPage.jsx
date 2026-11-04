import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ProjectDetails from '../components/projects/ProjectDetails.jsx';
import ProjectForm from '../components/projects/ProjectForm.jsx';
import { ProjectList } from '../components/projects/ProjectList.jsx';
import { useProjects } from '../hooks/useProjects.js';

export function ProjectsPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const {
    projects,
    isLoading,
    error: loadError,
    reload,
    saveProject,
    deleteProject,
  } = useProjects();
  const [form, setForm] = useState(null);
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);

  const searchText = search.toLocaleLowerCase('tr-TR');
  const filteredProjects = projects.filter((project) => {
    const projectText = `${project.name} ${project.environment}`.toLocaleLowerCase('tr-TR');
    return projectText.includes(searchText);
  });

  let selectedProject = filteredProjects[0];
  if (projectId) {
    selectedProject = projects.find((project) => project.id === projectId);
  }

  const projectNotFound = Boolean(projectId) && !selectedProject && !isLoading && !loadError;
  const activeProfileCount = projects.filter((project) => project.enabled).length;

  function openCreateForm() {
    setNotice('');
    setForm({ project: null });
  }

  async function handleSave(request) {
    const saved = await saveProject(form.project?.id, request);
    setForm(null);
    setSearch('');
    setError('');
    setNotice('Proje profili kaydedildi.');
    navigate(`/projects/${saved.id}`);
  }

  async function handleDelete(project) {
    const confirmed = window.confirm(
      `“${project.name}” profilini silmek istiyor musunuz? Bu işlem izlenen uygulamayı silmez.`,
    );
    if (!confirmed) {
      return;
    }

    setIsDeleting(true);
    setError('');
    setNotice('');

    try {
      await deleteProject(project.id);
      setNotice('Proje profili silindi.');
      navigate('/projects');
    } catch (failure) {
      setError(failure.message);
    } finally {
      setIsDeleting(false);
    }
  }

  function renderProjectContent() {
    if (isLoading && projects.length === 0) {
      return (
        <p className="empty-state" role="status">
          Projeler yükleniyor…
        </p>
      );
    }

    if (loadError && projects.length === 0) {
      return (
        <button className="button secondary" onClick={reload}>
          Projeleri tekrar yükle
        </button>
      );
    }

    if (projects.length === 0 && !projectNotFound) {
      return (
        <section className="empty-state">
          <h2>İlk projenizi ekleyin</h2>
          <p className="muted">Bir ad, repository adresi ve ortam bilgisiyle başlayabilirsiniz.</p>
          <button className="button primary" onClick={openCreateForm}>
            Yeni proje ekle
          </button>
        </section>
      );
    }

    return (
      <>
        <ProjectList projects={filteredProjects} selectedId={selectedProject?.id} />
        {projectNotFound && (
          <p className="notice error" role="alert">
            İstenen proje bulunamadı. <Link to="/projects">Proje listesine dön</Link>
          </p>
        )}
        {selectedProject && (
          <ProjectDetails
            project={selectedProject}
            deleting={isDeleting}
            onEdit={() => setForm({ project: selectedProject })}
            onDelete={() => handleDelete(selectedProject)}
          />
        )}
      </>
    );
  }

  return (
    <div className="workspace-content">
      <header className="page-heading">
        <div>
          <p className="eyebrow">PROJE YÖNETİMİ</p>
          <h1>Projeler</h1>
          <p className="muted">
            İzleyeceğiniz uygulamaları ve bağlantılarını tek yerden tanımlayın.
          </p>
        </div>
        <button className="button primary" onClick={openCreateForm}>
          ＋ Yeni proje
        </button>
      </header>
      {(error || loadError) && (
        <p className="notice error" role="alert">
          {error || loadError}
        </p>
      )}
      {notice && (
        <p className="notice success" role="status">
          {notice}
        </p>
      )}
      <div className="summary-strip">
        <div>
          <span className="summary-value">{String(projects.length).padStart(2, '0')}</span>
          <span>Kayıtlı proje</span>
        </div>
        <div>
          <span className="summary-value">{String(activeProfileCount).padStart(2, '0')}</span>
          <span>Aktif profil</span>
        </div>
        <div className="summary-explainer">
          <strong>Bağlantılara hazır</strong>
          <span>Bu sürümde proje yapılandırması yönetilir.</span>
        </div>
      </div>
      <div className="projects-toolbar">
        <h2>
          Proje listesi <span>{projects.length}</span>
        </h2>
        <div>
          <label className="search-field">
            <span className="sr-only">Proje ara</span>
            <input
              type="search"
              placeholder="Proje veya ortam ara…"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <button
            className="button secondary"
            disabled={isLoading}
            onClick={() => {
              setNotice('');
              reload();
            }}
          >
            {isLoading ? 'Yükleniyor…' : '↻ Yenile'}
          </button>
        </div>
      </div>
      {renderProjectContent()}
      <footer className="workspace-footer">
        <span>OpsBridge</span>
        <span>Proje profilleri · İlk sürüm</span>
      </footer>
      {form && (
        <ProjectForm project={form.project} onSave={handleSave} onClose={() => setForm(null)} />
      )}
    </div>
  );
}
