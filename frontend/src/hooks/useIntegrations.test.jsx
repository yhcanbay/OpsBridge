import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { projectService } from '../services/projectService.js'
import { useIntegrations } from './useIntegrations.js'

vi.mock('../services/projectService.js', () => ({
    projectService: { getIntegrationStatuses: vi.fn() }
}))
afterEach(cleanup)

it('eski URL kontrolünden geç gelen başarılı yanıt yeni profilin bağlantı hatasını örtemez', async () => {
    let resolveOldRequest
    projectService.getIntegrationStatuses
        .mockImplementationOnce(
            () =>
                new Promise((resolve) => {
                    resolveOldRequest = resolve
                })
        )
        .mockResolvedValueOnce([{ integration: 'Jenkins', state: 'JOB_NOT_FOUND' }])
    const { result, rerender } = renderHook(
        ({ updatedAt }) => useIntegrations('project-1', updatedAt),
        { initialProps: { updatedAt: 'old' } }
    )
    rerender({ updatedAt: 'new' })
    await waitFor(() => expect(result.current.items[0]?.state).toBe('JOB_NOT_FOUND'))
    await act(async () => resolveOldRequest([{ integration: 'Jenkins', state: 'CONNECTED' }]))
    expect(result.current.items[0].state).toBe('JOB_NOT_FOUND')
})
