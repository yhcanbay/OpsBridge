import { useEffect, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

export function useGitHistory(projectId, query) {
    const [history, setHistory] = useState(null)
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        setHistory(null)
        setIsLoading(true)
        setError('')

        async function loadGitHistory() {
            try {
                const result = await projectService.getGitHistory(projectId, query, {
                    signal: controller.signal
                })
                if (!controller.signal.aborted) {
                    setHistory(result)
                }
            } catch (failure) {
                if (!controller.signal.aborted && !isRequestCancelled(failure)) {
                    setError(failure.message)
                }
            } finally {
                if (!controller.signal.aborted) {
                    setIsLoading(false)
                }
            }
        }

        loadGitHistory()

        return () => {
            controller.abort()
        }
    }, [projectId, query])

    return { history, isLoading, error }
}
