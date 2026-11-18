import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { projectService } from '../services/projectService.js'
import DockerDetailsPage from './DockerDetailsPage.jsx'

vi.mock('../services/projectService.js', () => ({
    projectService: { getProjectById: vi.fn(), getDockerContainers: vi.fn() }
}))

const containers = [
    {
        id: 'b13e49a7c602-test',
        serviceName: 'backend',
        name: 'taskapp-backend',
        state: 'exited',
        health: 'none',
        image: 'taskapp-backend:latest',
        imageId: 'sha256:backend',
        startedAt: '2026-11-18T11:18:00Z',
        finishedAt: '2026-11-18T11:24:00Z',
        exitCode: 0,
        ports: []
    },
    {
        id: 'c792bf90e314-test',
        serviceName: 'db',
        name: 'taskapp-db',
        state: 'running',
        health: 'healthy',
        image: 'postgres:16-alpine',
        imageId: 'sha256:db',
        startedAt: '2026-11-18T11:18:00Z',
        finishedAt: null,
        exitCode: null,
        ports: ['5432 → 5432/tcp']
    }
]

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/projects/project-1/docker']}>
            <Routes>
                <Route path="/projects/:projectId/docker" element={<DockerDetailsPage />} />
            </Routes>
        </MemoryRouter>
    )
}

beforeEach(() => {
    projectService.getProjectById.mockResolvedValue({
        name: 'Personal_Task_App',
        containerLabelSelector: 'com.docker.compose.project=personal_task_app'
    })
    projectService.getDockerContainers.mockResolvedValue(containers)
})

afterEach(cleanup)

describe('Docker ayrıntı sayfası', () => {
    it('container bilgilerini gösterir ve durum filtresi seçili ayrıntıyı günceller', async () => {
        renderPage()
        expect(await screen.findByText('1 container durmuş')).toBeTruthy()
        const details = within(screen.getByRole('region', { name: 'Container ayrıntıları' }))
        expect(details.getByText('sha256:backend')).toBeTruthy()
        expect(details.getByText('0')).toBeTruthy()
        expect(details.getByText(/14:24/)).toBeTruthy()

        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'running' } })
        expect(screen.queryByRole('button', { name: /taskapp-backend/ })).toBeNull()
        expect(details.getByText('sha256:db')).toBeTruthy()
        expect(details.getByText('Healthy')).toBeTruthy()
        expect(details.getByText('5432 → 5432/tcp')).toBeTruthy()

        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'all' } })
        fireEvent.click(screen.getByRole('button', { name: /taskapp-db/ }))
        expect(details.getByText('sha256:db')).toBeTruthy()
    })

    it('başarısız yenilemede eski container listesini kaldırır', async () => {
        renderPage()
        expect(await screen.findByRole('table')).toBeTruthy()
        projectService.getDockerContainers.mockRejectedValueOnce(new Error('Docker Engine kapalı.'))
        fireEvent.click(screen.getByRole('button', { name: 'Yenile' }))
        expect(await screen.findByRole('alert')).toHaveProperty(
            'textContent',
            'Docker Engine kapalı.'
        )
        expect(screen.queryByRole('table')).toBeNull()
    })

    it('bağlantı kurulup eşleşen container bulunmadığında boş durumu gösterir', async () => {
        projectService.getDockerContainers.mockResolvedValueOnce([])
        renderPage()
        expect(await screen.findByRole('heading', { name: 'Container bulunamadı' })).toBeTruthy()
        expect(screen.queryByRole('alert')).toBeNull()
    })
})
