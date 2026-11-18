import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/ApiError.js'
import AuthProvider from '../context/AuthProvider.jsx'
import { authService } from '../services/authService.js'
import { githubConnectionService } from '../services/githubConnectionService.js'
import { projectService } from '../services/projectService.js'
import App from './App.jsx'

vi.mock('../services/authService.js', () => ({
    authService: {
        getCurrentUser: vi.fn(),
        login: vi.fn(),
        logout: vi.fn()
    }
}))

vi.mock('../services/projectService.js', () => ({
    projectService: {
        getAllProjects: vi.fn(),
        getProjectById: vi.fn(),
        saveProject: vi.fn(),
        updateProject: vi.fn(),
        deleteProject: vi.fn(),
        getIntegrationStatuses: vi.fn(),
        getGitHistory: vi.fn()
    }
}))

vi.mock('../services/githubConnectionService.js', () => ({
    githubConnectionService: { getAllConnections: vi.fn() }
}))

const project = {
    id: 'reference-project',
    name: 'Örnek proje',
    environment: 'local',
    repositoryUrl: 'https://github.com/example/project',
    capabilities: [],
    enabled: true,
    updatedAt: '2026-11-19T10:00:00Z'
}

beforeAll(() => {
    HTMLDialogElement.prototype.showModal = function () {
        this.open = true
    }
    HTMLDialogElement.prototype.close = function () {
        this.open = false
    }
})

beforeEach(() => {
    vi.resetAllMocks()
    authService.getCurrentUser.mockResolvedValue({ username: 'admin', role: 'ADMIN' })
    githubConnectionService.getAllConnections.mockResolvedValue([])
    projectService.getAllProjects.mockResolvedValue([project])
    projectService.getProjectById.mockResolvedValue(project)
    projectService.getIntegrationStatuses.mockResolvedValue([])
    projectService.getGitHistory.mockResolvedValue({
        branches: [],
        commits: [],
        branchPage: 1,
        fetchedAt: project.updatedAt
    })
})

afterEach(cleanup)

function renderRoute(path) {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <AuthProvider>
                <App />
            </AuthProvider>
        </MemoryRouter>
    )
}

describe('authentication and route navigation', () => {
    it('returns to the requested project URL after logging in', async () => {
        authService.getCurrentUser.mockRejectedValue(new ApiError(401, 'Oturum bulunamadı.'))
        authService.login.mockResolvedValue({ username: 'admin', role: 'ADMIN' })
        renderRoute('/projects/reference-project')

        const username = await screen.findByLabelText('Kullanıcı adı')
        fireEvent.change(username, { target: { value: 'admin' } })
        fireEvent.change(screen.getByLabelText('Parola'), { target: { value: 'test-only' } })
        fireEvent.click(screen.getByRole('button', { name: 'Giriş yap →' }))

        expect(await screen.findByRole('heading', { name: project.name })).toBeTruthy()
        expect(projectService.getProjectById).toHaveBeenCalledWith(project.id, {
            signal: expect.any(AbortSignal)
        })
        expect(projectService.getAllProjects).not.toHaveBeenCalled()
        expect(authService.login).toHaveBeenCalledWith({ username: 'admin', password: 'test-only' })
    })

    it('offers retry for a connection failure instead of treating it as logout', async () => {
        authService.getCurrentUser.mockRejectedValueOnce(new ApiError(0, 'Backend erişilemiyor.'))
        authService.getCurrentUser.mockResolvedValueOnce({ username: 'admin', role: 'ADMIN' })
        renderRoute('/projects')

        expect(
            await screen.findByRole('heading', { name: 'Oturum kontrol edilemedi' })
        ).toBeTruthy()
        expect(screen.queryByLabelText('Parola')).toBeNull()
        fireEvent.click(screen.getByRole('button', { name: 'Tekrar dene' }))

        await waitFor(() => expect(screen.getByRole('heading', { name: 'Projeler' })).toBeTruthy())
    })

    it('renders a missing-page view for unknown routes', async () => {
        authService.getCurrentUser.mockResolvedValue({ username: 'admin', role: 'ADMIN' })
        renderRoute('/unknown')
        expect(await screen.findByRole('heading', { name: 'Sayfa bulunamadı' })).toBeTruthy()
    })
})

describe('proje listesi ve detay sayfası', () => {
    it('listeyi açar ve ayrıntıları proje bağlantısı seçildikten sonra yükler', async () => {
        renderRoute('/')
        const projectLink = await screen.findByRole('link', { name: project.name })
        expect(projectLink.getAttribute('href')).toBe('/projects/reference-project')
        expect(screen.queryByText(project.repositoryUrl)).toBeNull()
        expect(projectService.getProjectById).not.toHaveBeenCalled()
        expect(projectService.getIntegrationStatuses).not.toHaveBeenCalled()
        expect(projectService.getGitHistory).not.toHaveBeenCalled()

        fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'local' } })
        expect(screen.queryByRole('link', { name: project.name })).toBeNull()
        fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'ÖRNEK' } })
        fireEvent.click(screen.getByRole('link', { name: project.name }))

        expect(await screen.findByRole('heading', { name: project.name })).toBeTruthy()
        expect(screen.queryByRole('searchbox')).toBeNull()
        expect(projectService.getProjectById).toHaveBeenCalledTimes(1)
        fireEvent.click(screen.getByRole('link', { name: 'Proje listesine dön' }))
        expect(await screen.findByRole('link', { name: project.name })).toBeTruthy()
    })

    it('detayı yükleme hatasında tekrar dener ve bulunamayan profilden listeye döner', async () => {
        projectService.getProjectById
            .mockRejectedValueOnce(new ApiError(0, 'Backend erişilemiyor.'))
            .mockRejectedValueOnce(new ApiError(404, 'Profil bulunamadı.'))
        renderRoute('/projects/silinmis-proje')

        expect((await screen.findByRole('alert')).textContent).toBe('Backend erişilemiyor.')
        fireEvent.click(screen.getByRole('button', { name: 'Tekrar dene' }))
        expect((await screen.findByRole('alert')).textContent).toBe('Proje bulunamadı.')
        fireEvent.click(screen.getByRole('link', { name: 'Proje listesine dön' }))
        expect(await screen.findByRole('heading', { name: 'Projeler' })).toBeTruthy()
    })

    it('ekleme, düzenleme ve silme sonrasında ilgili ekrana gider', async () => {
        const createdProject = { ...project, id: 'created', name: 'Yeni izlenen proje' }
        projectService.saveProject.mockResolvedValue(createdProject)
        projectService.getProjectById.mockResolvedValue(createdProject)
        projectService.updateProject.mockResolvedValue({
            ...createdProject,
            name: 'Düzenlenen proje'
        })
        projectService.deleteProject.mockResolvedValue(undefined)
        vi.spyOn(window, 'confirm').mockReturnValue(true)
        renderRoute('/projects')

        fireEvent.click(await screen.findByRole('button', { name: 'Yeni proje' }))
        const createForm = within(screen.getByRole('dialog'))
        fireEvent.change(createForm.getByLabelText(/Proje adı/), {
            target: { value: createdProject.name }
        })
        fireEvent.change(createForm.getByLabelText(/Repository URL/), {
            target: { value: project.repositoryUrl }
        })
        fireEvent.click(createForm.getByRole('button', { name: 'Profili kaydet' }))
        expect(await screen.findByRole('heading', { name: createdProject.name })).toBeTruthy()
        expect(await screen.findByText('Proje profili kaydedildi.')).toBeTruthy()

        fireEvent.click(screen.getByRole('button', { name: 'Profili düzenle' }))
        fireEvent.change(screen.getByLabelText(/Proje adı/), {
            target: { value: 'Düzenlenen proje' }
        })
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))
        expect(await screen.findByRole('heading', { name: 'Düzenlenen proje' })).toBeTruthy()
        expect(screen.queryByRole('dialog')).toBeNull()
        expect(projectService.updateProject).toHaveBeenCalledWith(
            createdProject.id,
            expect.objectContaining({ name: 'Düzenlenen proje' })
        )

        fireEvent.click(screen.getByRole('button', { name: 'Profili sil' }))
        expect(await screen.findByText('Proje profili silindi.')).toBeTruthy()
        expect(screen.getByRole('heading', { name: 'Projeler' })).toBeTruthy()
        expect(projectService.deleteProject).toHaveBeenCalledWith(createdProject.id)
    })
})
