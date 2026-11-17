import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/ApiError.js'
import { projectService } from '../services/projectService.js'
import JenkinsDetailsPage from './JenkinsDetailsPage.jsx'

vi.mock('../services/projectService.js', () => ({
    projectService: { getProjectById: vi.fn(), getJenkinsJobInfo: vi.fn() }
}))

const project = {
    id: 'project-1',
    name: 'Task App',
    repositoryUrl: 'https://github.com/example/task-app',
    environment: 'local'
}
const completeBuild = {
    number: 47,
    result: 'FAILURE',
    startedAt: '2026-11-14T10:00:00Z',
    durationMillis: 72000,
    url: 'http://jenkins.test/job/app/47/',
    consoleUrl: 'http://jenkins.test/job/app/47/console',
    changes: []
}

function createJob() {
    const runningBuild = {
        ...completeBuild,
        number: 48,
        result: null,
        building: true,
        commitSha: 'a8f3c210123456789012345678901234567890123',
        branch: 'main'
    }
    return {
        jobName: 'task-app',
        jobUrl: 'http://jenkins.test/job/app/',
        lastBuild: runningBuild,
        lastCompletedBuild: completeBuild,
        lastSuccessfulBuild: { ...completeBuild, number: 46, result: 'SUCCESS' },
        selectedBuild: runningBuild,
        builds: [completeBuild, { ...completeBuild, number: 46, result: 'SUCCESS' }],
        stages: [
            { name: 'Checkout', status: 'SUCCESS', durationMillis: 4000 },
            { name: 'Frontend', status: 'IN_PROGRESS', durationMillis: 18000 }
        ],
        testReport: null,
        testReportMessage: 'Test raporu bulunamadı. Test sonuçları bilinmiyor.',
        fetchedAt: '2026-11-14T10:01:00Z'
    }
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/projects/project-1/jenkins']}>
            <Routes>
                <Route path="/projects/:projectId/jenkins" element={<JenkinsDetailsPage />} />
            </Routes>
        </MemoryRouter>
    )
}

beforeEach(() => {
    projectService.getProjectById.mockResolvedValue(project)
    projectService.getJenkinsJobInfo.mockResolvedValue(createJob())
})
afterEach(cleanup)

describe('Jenkins ayrıntıları', () => {
    it('devam eden build ile önceki tamamlanmış sonucu ayrı gösterir', async () => {
        renderPage()
        expect(await screen.findByRole('heading', { name: 'Pipeline çalışıyor.' })).toBeTruthy()
        expect(screen.getByText('Son tamamlanan build')).toBeTruthy()
        expect(screen.getByRole('button', { name: '#47' })).toBeTruthy()
        expect(screen.getByText('Frontend aşaması devam ediyor.')).toBeTruthy()
        expect(screen.queryByText('Tasarım önizlemesi')).toBeNull()
    })

    it('rapor yokken sıfır başarısız test sonucu üretmez', async () => {
        const { container } = renderPage()
        expect(
            await screen.findByText('Test raporu bulunamadı. Test sonuçları bilinmiyor.')
        ).toBeTruthy()
        expect(container.querySelector('.jd-test-numbers')).toBeNull()
    })

    it('geçmişi filtreler ve seçilen build için backend isteği gönderir', async () => {
        projectService.getJenkinsJobInfo.mockImplementation(async (id, build) => {
            const job = createJob()
            if (build === '47') job.selectedBuild = completeBuild
            return job
        })
        renderPage()
        await screen.findByRole('heading', { name: 'Pipeline çalışıyor.' })
        fireEvent.change(screen.getByLabelText('Build sonucuna göre filtrele'), {
            target: { value: 'failed' }
        })
        expect(screen.queryByRole('button', { name: '#46' })).toBeNull()
        fireEvent.click(screen.getByRole('button', { name: '#47' }))
        expect(
            await screen.findByRole('heading', { name: 'Pipeline başarısız tamamlandı.' })
        ).toBeTruthy()
        expect(projectService.getJenkinsJobInfo).toHaveBeenLastCalledWith(
            'project-1',
            '47',
            expect.any(Object)
        )
        fireEvent.click(screen.getByRole('button', { name: "Son build'e dön" }))
        expect(await screen.findByRole('heading', { name: 'Pipeline çalışıyor.' })).toBeTruthy()
    })

    it('yenileme hatasında eski başarılı veriyi ekranda bırakmaz', async () => {
        projectService.getJenkinsJobInfo
            .mockResolvedValueOnce(createJob())
            .mockRejectedValueOnce(new ApiError(502, "Jenkins'e ulaşılamıyor."))
        renderPage()
        await screen.findByRole('heading', { name: 'Pipeline çalışıyor.' })
        fireEvent.click(screen.getByRole('button', { name: 'Yenile' }))
        expect(await screen.findByRole('alert')).toHaveProperty(
            'textContent',
            "Jenkins'e ulaşılamıyor."
        )
        expect(screen.queryByText('Jenkins bağlı')).toBeNull()
        expect(screen.queryByRole('heading', { name: 'Pipeline çalışıyor.' })).toBeNull()
    })

    it('henüz build çalışmamış job için boş durumu gösterir', async () => {
        projectService.getJenkinsJobInfo.mockResolvedValue({
            ...createJob(),
            lastBuild: null,
            selectedBuild: null,
            builds: []
        })
        renderPage()
        expect(await screen.findByRole('heading', { name: 'İlk build bekleniyor' })).toBeTruthy()
        expect(screen.queryByText('Başarı oranı')).toBeNull()
    })

    it('aşama API bulunmadığında build sonucunu göstermeyi sürdürür', async () => {
        projectService.getJenkinsJobInfo.mockResolvedValue({
            ...createJob(),
            stages: [],
            stagesMessage: 'Pipeline REST API bulunamadı.'
        })
        renderPage()
        expect(await screen.findByText('Pipeline REST API bulunamadı.')).toBeTruthy()
        expect(screen.getByRole('heading', { name: 'Pipeline çalışıyor.' })).toBeTruthy()
    })

    it('bekleyen istek sayfadan ayrılınca iptal edilir', async () => {
        let resolveJob
        projectService.getJenkinsJobInfo.mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveJob = resolve
                })
        )
        const view = renderPage()
        await waitFor(() => expect(projectService.getJenkinsJobInfo).toHaveBeenCalled())
        const options = projectService.getJenkinsJobInfo.mock.calls.at(-1)[2]
        view.unmount()
        expect(options.signal.aborted).toBe(true)
        await act(async () => resolveJob(createJob()))
    })
})
