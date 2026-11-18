import { useCallback, useEffect, useRef, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

function sortProjects(projects) {
    return [...projects].sort((first, second) => first.name.localeCompare(second.name, 'tr'))
}

export function useProjects() {
    const [projects, setProjects] = useState([])
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')
    const activeRequest = useRef(null)

    const reload = useCallback(async () => {
        activeRequest.current?.abort()
        const controller = new AbortController()
        activeRequest.current = controller
        setIsLoading(true)
        setError('')

        try {
            const projectList = await projectService.getAllProjects({ signal: controller.signal })
            if (!controller.signal.aborted) {
                setProjects(sortProjects(projectList))
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
    }, [])

    useEffect(() => {
        reload()

        return () => {
            activeRequest.current?.abort()
        }
    }, [reload])

    return { projects, isLoading, error, reload }
}
