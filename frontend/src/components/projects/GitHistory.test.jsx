import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { projectService } from '../../services/projectService.js'
import GitHistory from './GitHistory.jsx'
import ProjectDetails from './ProjectDetails.jsx'

vi.mock('../../services/projectService.js', () => ({
    projectService: { getGitHistory: vi.fn() }
}))

vi.mock('./IntegrationCards.jsx', () => ({ default: () => <div>Bağlantı kartları</div> }))

const history = {
    branches: ['main', 'feature/git'],
    branchPage: 1,
    hasMoreBranches: true,
    selectedBranch: 'main',
    commits: [
        {
            sha: '0123456789012345678901234567890123456789',
            message: 'Git geçmişi eklendi',
            author: 'Example',
            committedAt: '2026-11-13T10:00:00Z'
        }
    ],
    page: 1,
    hasMoreCommits: true,
    fetchedAt: '2026-11-13T11:00:00Z'
}

beforeEach(() => {
    vi.resetAllMocks()
    projectService.getGitHistory.mockResolvedValue(history)
})

afterEach(cleanup)

describe('GitHub geçmişi', () => {
    it('branch, commit, tam SHA ve son başarılı okuma zamanını gösterir', async () => {
        render(<GitHistory projectId="project-1" />)
        expect(screen.getByRole('status').textContent).toContain('yükleniyor')

        expect(await screen.findByText('Git geçmişi eklendi')).toBeTruthy()
        expect(screen.getByLabelText('Branch').value).toBe('main')
        expect(screen.getByTitle(history.commits[0].sha).textContent).toBe('0123456')
        expect(screen.getByText(/Son başarılı okuma/)).toBeTruthy()
        expect(screen.getByText(/Çalışan Docker sürümünün commit bilgisi/)).toBeTruthy()
        expect(projectService.getGitHistory).toHaveBeenCalledWith(
            'project-1',
            { branch: '', page: 1, branchPage: 1 },
            { signal: expect.any(AbortSignal) }
        )
    })

    it('commit sayfasını değiştirir ve branch değişince ilk sayfaya döner', async () => {
        projectService.getGitHistory.mockImplementation(async (projectId, query) => ({
            ...history,
            selectedBranch: query.branch || 'main',
            page: query.page
        }))
        render(<GitHistory projectId="project-1" />)
        await screen.findByText('Git geçmişi eklendi')

        fireEvent.click(screen.getByRole('button', { name: 'Sonraki commit’ler' }))
        expect(await screen.findByText('Commit sayfası 2')).toBeTruthy()
        fireEvent.change(screen.getByLabelText('Branch'), { target: { value: 'feature/git' } })

        await waitFor(() => expect(screen.getByLabelText('Branch').value).toBe('feature/git'))
        expect(screen.getByText('Commit sayfası 1')).toBeTruthy()
        expect(projectService.getGitHistory).toHaveBeenLastCalledWith(
            'project-1',
            { branch: 'feature/git', page: 1, branchPage: 1 },
            { signal: expect.any(AbortSignal) }
        )
    })

    it('sonraki branch sayfasını açar ve devam yoksa düğmeleri kapatır', async () => {
        projectService.getGitHistory.mockResolvedValueOnce(history).mockResolvedValueOnce({
            ...history,
            branches: ['release/v1'],
            selectedBranch: 'release/v1',
            branchPage: 2,
            hasMoreBranches: false,
            hasMoreCommits: false
        })
        render(<GitHistory projectId="project-1" />)
        await screen.findByText('Git geçmişi eklendi')
        fireEvent.click(screen.getByRole('button', { name: 'Sonraki branch’ler' }))

        expect(await screen.findByText('Branch sayfası 2')).toBeTruthy()
        expect(screen.getByLabelText('Branch').value).toBe('release/v1')
        expect(screen.getByRole('button', { name: 'Sonraki branch’ler' }).disabled).toBe(true)
        expect(screen.getByRole('button', { name: 'Sonraki commit’ler' }).disabled).toBe(true)
        expect(projectService.getGitHistory).toHaveBeenLastCalledWith(
            'project-1',
            { branch: '', page: 1, branchPage: 2 },
            { signal: expect.any(AbortSignal) }
        )
    })

    it('yenileme hatasında eski sonucu temizler ve tekrar okumayı sağlar', async () => {
        projectService.getGitHistory
            .mockResolvedValueOnce(history)
            .mockRejectedValueOnce(new Error('GitHub istek sınırına ulaşıldı.'))
            .mockResolvedValueOnce(history)
        render(<GitHistory projectId="project-1" />)
        await screen.findByText('Git geçmişi eklendi')
        fireEvent.click(screen.getByRole('button', { name: 'Yenile' }))

        expect((await screen.findByRole('alert')).textContent).toContain('istek sınırına')
        expect(screen.queryByText('Git geçmişi eklendi')).toBeNull()
        expect(screen.queryByText(/Son başarılı okuma/)).toBeNull()
        fireEvent.click(screen.getByRole('button', { name: 'Yenile' }))
        expect(await screen.findByText('Git geçmişi eklendi')).toBeTruthy()
    })

    it('proje değişince eski isteği iptal eder ve geciken sonucunu göstermez', async () => {
        let resolveOldRequest
        projectService.getGitHistory.mockImplementationOnce(
            () => new Promise((resolve) => (resolveOldRequest = resolve))
        )
        const view = render(<GitHistory key="old" projectId="old" />)
        const oldSignal = projectService.getGitHistory.mock.calls[0][2].signal

        view.rerender(<GitHistory key="new" projectId="new" />)
        expect(await screen.findByText('Git geçmişi eklendi')).toBeTruthy()
        expect(oldSignal.aborted).toBe(true)

        await act(async () => {
            resolveOldRequest({
                ...history,
                commits: [{ ...history.commits[0], message: 'Eski projenin sonucu' }]
            })
        })
        expect(screen.queryByText('Eski projenin sonucu')).toBeNull()
        expect(screen.getByText('Git geçmişi eklendi')).toBeTruthy()
    })

    it('boş depoda boş durumu gösterir', async () => {
        projectService.getGitHistory.mockResolvedValue({
            ...history,
            branches: [],
            commits: [],
            selectedBranch: '',
            hasMoreBranches: false,
            hasMoreCommits: false
        })
        render(<GitHistory projectId="project-1" />)

        expect(await screen.findByText('Bu sayfada branch bulunamadı.')).toBeTruthy()
        expect(screen.getByText('Bu sayfada commit bulunamadı.')).toBeTruthy()
        expect(screen.queryByLabelText('Branch')).toBeNull()
    })

    it('pasif projenin detayında GitHub isteği göndermez', () => {
        render(
            <ProjectDetails
                project={{
                    id: 'disabled',
                    name: 'Pasif proje',
                    repositoryUrl: 'https://github.com/example/repo',
                    enabled: false,
                    capabilities: []
                }}
            />
        )
        expect(screen.getByText('Proje pasif. GitHub geçmişi okunmuyor.')).toBeTruthy()
        expect(projectService.getGitHistory).not.toHaveBeenCalled()
    })
})
