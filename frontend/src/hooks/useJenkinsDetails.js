import { useEffect, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

export function useJenkinsDetails(projectId, buildNumber) {
    const [project, setProject] = useState(null)
    const [job, setJob] = useState(null)
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')
    const [refreshIndex, setRefreshIndex] = useState(0)

    useEffect(() => {
        const controller = new AbortController()
        setProject(null)
        setJob(null)
        setIsLoading(true)
        setError('')

        async function loadJenkinsDetails() {
            try {
                const [loadedProject, loadedJob] = await Promise.all([
                    projectService.getProjectById(projectId, { signal: controller.signal }),
                    projectService.getJenkinsJobInfo(projectId, buildNumber, {
                        signal: controller.signal
                    })
                ])
                if (!controller.signal.aborted) {
                    setProject(loadedProject)
                    setJob(loadedJob)
                }
            } catch (failure) {
                if (!controller.signal.aborted && !isRequestCancelled(failure)) {
                    setError(failure.message)
                    setIsLoading(false)
                    controller.abort()
                }
            } finally {
                if (!controller.signal.aborted) setIsLoading(false)
            }
        }

        loadJenkinsDetails()
        return () => controller.abort()
    }, [projectId, buildNumber, refreshIndex])

    function refresh() {
        setRefreshIndex((currentIndex) => currentIndex + 1)
    }

    return { project, job, isLoading, error, refresh }
}
