import { useCallback, useEffect, useRef, useState } from 'react';
import { isRequestCancelled } from '../api/httpClient.js';
import { projectService } from '../services/projectService.js';

function sortProjects(projects) {
  return [...projects].sort((first, second) => first.name.localeCompare(second.name, 'tr'));
}

export function useProjects() {
  const [projects, setProjects] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const activeRequest = useRef(null);

  const reload = useCallback(async () => {
    activeRequest.current?.abort();
    const controller = new AbortController();
    activeRequest.current = controller;
    setIsLoading(true);
    setError('');

    try {
      const result = await projectService.list({ signal: controller.signal });
      setProjects(sortProjects(result));
    } catch (failure) {
      if (!isRequestCancelled(failure)) {
        setError(failure.message);
      }
    } finally {
      if (!controller.signal.aborted) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    reload();
    return () => activeRequest.current?.abort();
  }, [reload]);

  async function saveProject(projectId, request) {
    let savedProject;

    if (projectId) {
      savedProject = await projectService.update(projectId, request);
    } else {
      savedProject = await projectService.create(request);
    }

    activeRequest.current?.abort();
    setIsLoading(false);
    setError('');
    setProjects((currentProjects) => {
      const remaining = currentProjects.filter((project) => project.id !== savedProject.id);
      return sortProjects([...remaining, savedProject]);
    });

    return savedProject;
  }

  async function deleteProject(projectId) {
    await projectService.remove(projectId);
    activeRequest.current?.abort();
    setIsLoading(false);
    setProjects((currentProjects) => currentProjects.filter((project) => project.id !== projectId));
  }

  return { projects, isLoading, error, reload, saveProject, deleteProject };
}
