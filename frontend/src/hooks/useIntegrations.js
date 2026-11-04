import { useEffect, useState } from 'react';
import { isRequestCancelled } from '../api/httpClient.js';
import { projectService } from '../services/projectService.js';

export function useIntegrations(projectId, profileUpdatedAt) {
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setIsLoading(true);
    setError('');

    async function load() {
      try {
        const result = await projectService.getIntegrations(projectId, {
          signal: controller.signal,
        });
        setItems(result);
      } catch (failure) {
        if (!isRequestCancelled(failure)) {
          setError(failure.message);
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    load();
    return () => controller.abort();
  }, [projectId, profileUpdatedAt]);

  return { items, isLoading, error };
}
