import { useEffect, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

export function useDockerDetails(projectId) {
    const [result, setResult] = useState(null)
    const [refreshIndex, setRefreshIndex] = useState(0)

    useEffect(() => {
        const controller = new AbortController()
        const options = { signal: controller.signal }

        async function loadDockerDetails() {
            try {
                const [project, containers] = await Promise.all([
                    projectService.getProjectById(projectId, options),
                    projectService.getDockerContainers(projectId, options)
                ])

                if (!controller.signal.aborted) {
                    setResult({
                        projectId,
                        refreshIndex,
                        project,
                        containers,
                        fetchedAt: new Date()
                    })
                }
            } catch (failure) {
                if (!controller.signal.aborted && !isRequestCancelled(failure)) {
                    setResult({ projectId, refreshIndex, error: failure.message })
                    controller.abort()
                }
            }
        }

        loadDockerDetails()
        return () => controller.abort()
    }, [projectId, refreshIndex])

    function refresh() {
        setRefreshIndex((currentIndex) => currentIndex + 1)
    }

    const currentResult =
        result?.projectId === projectId && result.refreshIndex === refreshIndex ? result : null

    return { result: currentResult, isLoading: currentResult === null, refresh }
}
