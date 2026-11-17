import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import JenkinsIcon from '../components/jenkins/JenkinsIcon.jsx'
import { useJenkinsDetails } from '../hooks/useJenkinsDetails.js'
import './JenkinsDetailsPage.css'

const statusLabels = {
    running: 'Devam ediyor',
    success: 'Başarılı',
    failed: 'Başarısız',
    unstable: 'Kararsız',
    aborted: 'İptal edildi',
    skipped: 'Çalıştırılmadı',
    paused: 'Onay bekliyor',
    unknown: 'Bilinmiyor'
}

function getStatus(result, building = false) {
    if (building || result === 'IN_PROGRESS') return 'running'
    if (result === 'SUCCESS') return 'success'
    if (result === 'FAILURE' || result === 'FAILED') return 'failed'
    if (result === 'UNSTABLE') return 'unstable'
    if (result === 'ABORTED') return 'aborted'
    if (result === 'NOT_BUILT' || result === 'NOT_EXECUTED') return 'skipped'
    if (result === 'PAUSED_PENDING_INPUT') return 'paused'
    return 'unknown'
}

function BuildStatus({ status }) {
    let icon = 'clock'
    if (status === 'success') icon = 'check'
    if (status === 'failed' || status === 'aborted') icon = 'close'
    if (status === 'unstable') icon = 'alert'

    return (
        <span className={`jd-status jd-${status}`}>
            <JenkinsIcon name={icon} size={13} />
            {statusLabels[status]}
        </span>
    )
}

function formatDuration(milliseconds) {
    if (milliseconds === null || milliseconds === undefined) return 'Bilinmiyor'
    const seconds = Math.floor(milliseconds / 1000)
    if (seconds < 60) return `${seconds} sn`
    const minutes = Math.floor(seconds / 60)
    return `${minutes} dk ${String(seconds % 60).padStart(2, '0')} sn`
}

function formatDate(value, includeDate = true) {
    if (!value) return 'Bilinmiyor'
    const options = { timeZone: 'Europe/Istanbul', hour: '2-digit', minute: '2-digit' }
    if (includeDate) {
        options.day = '2-digit'
        options.month = 'short'
    }
    return new Intl.DateTimeFormat('tr-TR', options).format(new Date(value))
}

function JenkinsDetailsPage() {
    const { projectId } = useParams()
    const [searchParams, setSearchParams] = useSearchParams()
    const selectedBuildNumber = searchParams.get('build')
    const { project, job, isLoading, error, refresh } = useJenkinsDetails(
        projectId,
        selectedBuildNumber
    )
    const [historyFilter, setHistoryFilter] = useState('all')

    function handleBuildSelect(number) {
        setSearchParams({ build: String(number) })
    }

    function handleLatestBuildSelect() {
        setSearchParams({})
    }

    function handleHistoryFilterChange(event) {
        setHistoryFilter(event.target.value)
    }

    function renderJobDetails() {
        const build = job.selectedBuild
        if (!build) {
            return (
                <section className="jd-card jd-empty" aria-labelledby="jd-empty-title">
                    <span className="jd-empty-symbol">
                        <JenkinsIcon name="pipeline" size={30} />
                    </span>
                    <span className="jd-overline">JENKINS BAĞLANTISI HAZIR</span>
                    <h2 id="jd-empty-title">İlk build bekleniyor</h2>
                    <p>
                        Bu job için henüz bir çalışma bulunmuyor. İlk build sonrasında ayrıntıları
                        burada görebilirsiniz.
                    </p>
                </section>
            )
        }

        const status = getStatus(build.result, build.building)
        const history = job.builds ?? []
        const stages = job.stages ?? []
        const changes = build.changes ?? []
        const report = job.testReport
        const successfulBuilds = history.filter((item) => getStatus(item.result) === 'success')
        const buildsWithDuration = history.filter(
            (item) => item.durationMillis !== null && item.durationMillis !== undefined
        )
        const visibleHistory = history.filter(
            (item) => historyFilter === 'all' || getStatus(item.result) === historyFilter
        )
        const completedStages = stages.filter((stage) => stage.status === 'SUCCESS')
        const failedStage = stages.find((stage) => getStatus(stage.status) === 'failed')
        const runningStage = stages.find(
            (stage) => getStatus(stage.status) === 'running' || getStatus(stage.status) === 'paused'
        )
        let successRate = '—'
        let averageDuration = null
        let headline = `Build #${build.number} · ${statusLabels[status]}`
        let description = 'Ayrıntılı çıktı Jenkins konsolundan incelenebilir.'
        let stageMessage = 'Jenkins tarafından bildirilen aşamalar'

        if (history.length > 0)
            successRate = `%${Math.round((successfulBuilds.length / history.length) * 100)}`
        if (buildsWithDuration.length > 0) {
            averageDuration =
                buildsWithDuration.reduce((total, item) => total + item.durationMillis, 0) /
                buildsWithDuration.length
        }
        if (status === 'running') {
            headline = 'Pipeline çalışıyor.'
            description = runningStage
                ? `${runningStage.name} aşaması devam ediyor.`
                : 'Jenkins bu build üzerinde çalışmaya devam ediyor.'
        }
        if (status === 'success') {
            headline = 'Pipeline başarıyla tamamlandı.'
            description = 'Jenkins bu build sonucunu başarılı olarak bildirdi.'
        }
        if (status === 'failed') {
            headline = 'Pipeline başarısız tamamlandı.'
            description = failedStage
                ? `${failedStage.name} aşamasında hata oluştu.`
                : 'Hata ayrıntıları Jenkins konsolundan incelenebilir.'
        }
        if (failedStage) stageMessage = `Hata noktası: ${failedStage.name}`
        else if (runningStage)
            stageMessage = `${runningStage.name} · ${statusLabels[getStatus(runningStage.status)]}`
        else if (stages.length > 0 && completedStages.length === stages.length)
            stageMessage = 'Bildirilen tüm aşamalar başarılı'

        return (
            <>
                <section
                    className={`jd-build-overview jd-overview-${status}`}
                    aria-labelledby="jd-build-heading"
                >
                    <div className="jd-build-headline">
                        <span className={`jd-build-symbol jd-${status}`}>
                            <JenkinsIcon
                                name={
                                    status === 'success'
                                        ? 'check'
                                        : status === 'failed'
                                          ? 'close'
                                          : 'play'
                                }
                                size={23}
                            />
                        </span>
                        <div>
                            <div className="jd-build-eyebrow">
                                {selectedBuildNumber ? 'SEÇİLİ ÇALIŞMA' : 'SON ÇALIŞMA'}
                                <span>#{build.number}</span>
                                <BuildStatus status={status} />
                            </div>
                            <h2 id="jd-build-heading">{headline}</h2>
                            <p>{description}</p>
                        </div>
                    </div>
                    <div className="jd-build-timing">
                        <div>
                            <span>{build.building ? 'Geçen süre' : 'Toplam süre'}</span>
                            <strong>{formatDuration(build.durationMillis)}</strong>
                        </div>
                        <div>
                            <span>Başlangıç · TRT</span>
                            <strong title={formatDate(build.startedAt)}>
                                {formatDate(build.startedAt, false)}
                            </strong>
                        </div>
                    </div>
                    {selectedBuildNumber && (
                        <button className="jd-return-current" onClick={handleLatestBuildSelect}>
                            Son build'e dön
                            <JenkinsIcon name="chevron" size={13} />
                        </button>
                    )}
                </section>

                <div className="jd-metrics" aria-label="Tamamlanmış build özeti">
                    <article>
                        <span className="jd-metric-label">
                            Son tamamlanan build
                            <JenkinsIcon name="pipeline" size={16} />
                        </span>
                        <div className="jd-metric-value">
                            {job.lastCompletedBuild
                                ? `#${job.lastCompletedBuild.number}`
                                : 'Henüz yok'}
                            {job.lastCompletedBuild && (
                                <BuildStatus status={getStatus(job.lastCompletedBuild.result)} />
                            )}
                        </div>
                        <small>
                            {job.lastCompletedBuild
                                ? formatDate(job.lastCompletedBuild.startedAt)
                                : 'Tamamlanmış bir çalışma bulunmuyor.'}
                        </small>
                    </article>
                    <article>
                        <span className="jd-metric-label">
                            Son başarılı build
                            <JenkinsIcon name="check" size={17} />
                        </span>
                        <div className="jd-metric-value">
                            {job.lastSuccessfulBuild
                                ? `#${job.lastSuccessfulBuild.number}`
                                : 'Henüz yok'}
                        </div>
                        <small>
                            {job.lastSuccessfulBuild
                                ? `${formatDate(job.lastSuccessfulBuild.startedAt)} · ${formatDuration(job.lastSuccessfulBuild.durationMillis)}`
                                : 'Başarılı bir çalışma bulunmuyor.'}
                        </small>
                    </article>
                    <article>
                        <span className="jd-metric-label">
                            Başarı oranı
                            <span className="jd-metric-scope">{history.length} BUILD</span>
                        </span>
                        <div className="jd-metric-value">
                            {successRate}
                            <span className="jd-metric-subvalue">
                                {successfulBuilds.length} / {history.length} başarılı
                            </span>
                        </div>
                        <div
                            className="jd-mini-history"
                            aria-label="Build sonuçları, eskiden yeniye"
                        >
                            {[...history].reverse().map((item) => (
                                <span
                                    key={item.number}
                                    className={`jd-${getStatus(item.result)}`}
                                    title={`#${item.number} · ${statusLabels[getStatus(item.result)]}`}
                                />
                            ))}
                        </div>
                        <small>Son {history.length} tamamlanmış build</small>
                    </article>
                    <article>
                        <span className="jd-metric-label">
                            Ortalama build süresi
                            <JenkinsIcon name="clock" size={16} />
                        </span>
                        <div className="jd-metric-value jd-metric-duration">
                            {formatDuration(averageDuration)}
                        </div>
                        <small>Süresi bilinen {buildsWithDuration.length} tamamlanmış build</small>
                    </article>
                </div>

                <div className="jd-detail-grid">
                    <section
                        className="jd-card jd-pipeline-card"
                        aria-labelledby="jd-pipeline-heading"
                    >
                        <header className="jd-card-header">
                            <div>
                                <h2 id="jd-pipeline-heading">Pipeline aşamaları</h2>
                                <p>
                                    Build #{build.number}
                                    <span>·</span>
                                    <JenkinsIcon name="branch" size={13} />
                                    {build.branch || 'Branch bilinmiyor'}
                                </p>
                            </div>
                            {stages.length > 0 && (
                                <span className="jd-stage-count">
                                    {completedStages.length} / {stages.length} tamamlandı
                                </span>
                            )}
                        </header>
                        {stages.length > 0 ? (
                            <>
                                <div className="jd-stage-scroll">
                                    <ol
                                        className="jd-stages"
                                        style={{ '--jd-stage-count': stages.length }}
                                    >
                                        {stages.map((stage, index) => {
                                            const stageStatus = getStatus(stage.status)
                                            return (
                                                <li
                                                    key={`${index}:${stage.name}`}
                                                    className={`jd-stage jd-${stageStatus}`}
                                                >
                                                    <span className="jd-stage-node">
                                                        {stageStatus === 'success' ? (
                                                            <JenkinsIcon name="check" size={17} />
                                                        ) : stageStatus === 'failed' ? (
                                                            <JenkinsIcon name="close" size={17} />
                                                        ) : stageStatus === 'running' ? (
                                                            <span className="jd-stage-spinner" />
                                                        ) : (
                                                            <span className="jd-stage-index">
                                                                {String(index + 1).padStart(2, '0')}
                                                            </span>
                                                        )}
                                                    </span>
                                                    <strong>{stage.name}</strong>
                                                    <span className="jd-stage-status">
                                                        {stageStatus === 'success'
                                                            ? 'Tamamlandı'
                                                            : statusLabels[stageStatus]}
                                                    </span>
                                                    <small>
                                                        {formatDuration(stage.durationMillis)}
                                                    </small>
                                                </li>
                                            )
                                        })}
                                    </ol>
                                </div>
                                <div className={`jd-stage-message jd-message-${status}`}>
                                    <JenkinsIcon name={failedStage ? 'alert' : 'info'} size={17} />
                                    <div>
                                        <strong>{stageMessage}</strong>
                                        <p>
                                            Yalnızca Jenkins tarafından bildirilen aşamalar
                                            gösterilir.
                                        </p>
                                    </div>
                                </div>
                            </>
                        ) : (
                            <div className="jd-section-empty">
                                <JenkinsIcon name="info" size={20} />
                                <strong>Aşama bilgisi gösterilemiyor</strong>
                                <p>
                                    {job.stagesMessage ||
                                        'Bu build için henüz aşama kaydı bulunmuyor.'}
                                </p>
                            </div>
                        )}
                        {job.stagesMessage && stages.length > 0 && (
                            <p className="jd-inline-note">{job.stagesMessage}</p>
                        )}
                        <footer className="jd-pipeline-footer">
                            <span>
                                <JenkinsIcon name="terminal" size={16} />
                                Ayrıntılı çıktı Jenkins konsolunda
                            </span>
                            <a
                                className="jd-text-button"
                                href={build.consoleUrl}
                                target="_blank"
                                rel="noreferrer"
                            >
                                Konsolu aç
                                <JenkinsIcon name="arrow" size={14} />
                            </a>
                        </footer>
                    </section>

                    <section className="jd-card jd-source-card" aria-labelledby="jd-source-heading">
                        <header className="jd-card-header">
                            <div>
                                <h2 id="jd-source-heading">
                                    <JenkinsIcon name="code" size={17} />
                                    Build'in kaynak kodu
                                </h2>
                                <p>Bu çalışmaya dahil edilen sürüm</p>
                            </div>
                        </header>
                        <dl className="jd-source-values">
                            <div>
                                <dt>Repository</dt>
                                <dd>
                                    <a
                                        className="jd-repository-name"
                                        href={project.repositoryUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                        title={project.repositoryUrl}
                                    >
                                        {project.repositoryUrl.split('/').filter(Boolean).pop()}
                                    </a>
                                </dd>
                            </div>
                            <div>
                                <dt>Branch</dt>
                                <dd>
                                    <span className="jd-branch-pill">
                                        <JenkinsIcon name="branch" size={12} />
                                        {build.branch || 'Bilinmiyor'}
                                    </span>
                                </dd>
                            </div>
                            <div>
                                <dt>Build commit'i</dt>
                                <dd>
                                    {build.commitUrl ? (
                                        <a
                                            className="jd-commit-button"
                                            href={build.commitUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            <JenkinsIcon name="commit" size={14} />
                                            {build.commitSha.slice(0, 10)}
                                            <JenkinsIcon name="arrow" size={12} />
                                        </a>
                                    ) : (
                                        <code>{build.commitSha?.slice(0, 10) || 'Bilinmiyor'}</code>
                                    )}
                                </dd>
                            </div>
                        </dl>
                        <div className="jd-changes-list">
                            {changes.length > 0 ? (
                                changes.slice(0, 3).map((change, index) => (
                                    <div
                                        className="jd-commit-message"
                                        key={`${index}:${change.commitId}`}
                                    >
                                        <span className="jd-commit-track" />
                                        <div>
                                            <strong>{change.message}</strong>
                                            <span>
                                                {change.author}
                                                {change.url && (
                                                    <>
                                                        {' '}
                                                        ·{' '}
                                                        <a
                                                            href={change.url}
                                                            target="_blank"
                                                            rel="noreferrer"
                                                        >
                                                            {change.commitId.slice(0, 7)} ↗
                                                        </a>
                                                    </>
                                                )}
                                            </span>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <p className="jd-inline-note">
                                    Bu build için değişiklik listesi bildirilmedi.
                                </p>
                            )}
                            {changes.length > 3 && (
                                <a
                                    className="jd-changes-more"
                                    href={`${build.url}changes`}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    Diğer değişiklikleri Jenkins'te aç ↗
                                </a>
                            )}
                        </div>
                        <div className="jd-version-notes">
                            <div>
                                <span>Branch'in güncel son commit'i</span>
                                <strong>Bilinmiyor</strong>
                            </div>
                            <div>
                                <span>Çalışan uygulamanın sürümü</span>
                                <strong>Bilinmiyor</strong>
                            </div>
                            <p>
                                <JenkinsIcon name="info" size={13} />
                                Bu bilgiler build commit'inden ayrı doğrulanır.
                            </p>
                        </div>
                    </section>

                    <section
                        className="jd-card jd-history-card"
                        aria-labelledby="jd-history-heading"
                    >
                        <header className="jd-card-header">
                            <div>
                                <h2 id="jd-history-heading">
                                    Build geçmişi
                                    <span className="jd-count-badge">{history.length}</span>
                                </h2>
                                <p>Son tamamlanmış çalışmalar · en fazla 10 build</p>
                            </div>
                            <label className="jd-history-filter">
                                <span className="jd-sr-only">Build sonucuna göre filtrele</span>
                                <select value={historyFilter} onChange={handleHistoryFilterChange}>
                                    <option value="all">Tüm sonuçlar</option>
                                    <option value="success">Başarılı</option>
                                    <option value="failed">Başarısız</option>
                                    <option value="unstable">Kararsız</option>
                                    <option value="aborted">İptal edildi</option>
                                </select>
                            </label>
                        </header>
                        <div className="jd-table-scroll">
                            <table className="jd-build-table">
                                <caption className="jd-sr-only">
                                    Son tamamlanmış build'ler. Ayrıntılar için build numarasını
                                    seçin.
                                </caption>
                                <thead>
                                    <tr>
                                        <th scope="col">Build</th>
                                        <th scope="col">Sonuç</th>
                                        <th scope="col">Commit</th>
                                        <th scope="col">Başlangıç · TRT</th>
                                        <th scope="col">Süre</th>
                                        <th scope="col">
                                            <span className="jd-sr-only">Jenkins bağlantısı</span>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {visibleHistory.map((item) => (
                                        <tr
                                            key={item.number}
                                            className={
                                                build.number === item.number
                                                    ? 'jd-selected-row'
                                                    : ''
                                            }
                                        >
                                            <td>
                                                <button
                                                    className="jd-build-number"
                                                    aria-pressed={build.number === item.number}
                                                    onClick={() => handleBuildSelect(item.number)}
                                                >
                                                    #{item.number}
                                                </button>
                                            </td>
                                            <td>
                                                <BuildStatus status={getStatus(item.result)} />
                                            </td>
                                            <td>
                                                <code>
                                                    {item.commitSha?.slice(0, 7) || 'Bilinmiyor'}
                                                </code>
                                            </td>
                                            <td>{formatDate(item.startedAt)}</td>
                                            <td>{formatDuration(item.durationMillis)}</td>
                                            <td>
                                                <a
                                                    className="jd-icon-button"
                                                    aria-label={`Build #${item.number} Jenkins ayrıntıları`}
                                                    href={item.url}
                                                    target="_blank"
                                                    rel="noreferrer"
                                                >
                                                    <JenkinsIcon name="arrow" size={16} />
                                                </a>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {visibleHistory.length === 0 && (
                                <p className="jd-history-empty">
                                    {history.length === 0
                                        ? 'Henüz tamamlanmış bir build yok.'
                                        : 'Bu filtreye uygun bir çalışma bulunamadı.'}
                                </p>
                            )}
                        </div>
                        <footer className="jd-table-footer">
                            <span>{visibleHistory.length} çalışma gösteriliyor</span>
                            <span>Devam eden build'ler listeye dahil değil.</span>
                        </footer>
                    </section>

                    <div className="jd-test-column">
                        <section
                            className="jd-card jd-tests-card"
                            aria-labelledby="jd-tests-heading"
                        >
                            <header className="jd-card-header">
                                <div>
                                    <h2 id="jd-tests-heading">
                                        <JenkinsIcon name="report" size={17} />
                                        Test özeti
                                    </h2>
                                    <p>Build #{build.number} · Yayımlanan test raporu</p>
                                </div>
                            </header>
                            {report ? (
                                <>
                                    <div className="jd-test-numbers">
                                        <div>
                                            <strong>{report.total}</strong>
                                            <span>Toplam test</span>
                                        </div>
                                        <div>
                                            <strong className="jd-test-passed">
                                                {report.passed}
                                            </strong>
                                            <span>Başarılı</span>
                                        </div>
                                        <div>
                                            <strong
                                                className={
                                                    report.failed > 0 ? 'jd-test-failed' : ''
                                                }
                                            >
                                                {report.failed}
                                            </strong>
                                            <span>Başarısız</span>
                                        </div>
                                    </div>
                                    {report.total > 0 && (
                                        <div
                                            className="jd-test-progress"
                                            role="img"
                                            aria-label={`${report.passed} başarılı, ${report.failed} başarısız, ${report.skipped} atlanan test`}
                                        >
                                            <span
                                                style={{
                                                    width: `${(report.passed / report.total) * 100}%`
                                                }}
                                            />
                                            <span
                                                className="jd-test-skipped"
                                                style={{
                                                    width: `${(report.skipped / report.total) * 100}%`
                                                }}
                                            />
                                        </div>
                                    )}
                                    {report.failedTests?.length > 0 ? (
                                        <div className="jd-failed-test">
                                            <span>BAŞARISIZ TESTLER · İLK 5</span>
                                            {report.failedTests.map((name) => (
                                                <code key={name}>{name}</code>
                                            ))}
                                        </div>
                                    ) : (
                                        <p className="jd-test-note">
                                            <JenkinsIcon name="report" size={14} />
                                            {report.skipped} test atlandı · Jenkins test raporu
                                        </p>
                                    )}
                                    <a
                                        className="jd-report-link"
                                        href={report.url}
                                        target="_blank"
                                        rel="noreferrer"
                                    >
                                        Test raporunu aç
                                        <JenkinsIcon name="arrow" size={14} />
                                    </a>
                                </>
                            ) : (
                                <div className="jd-no-report">
                                    <JenkinsIcon name="report" size={26} />
                                    <strong>Test raporu gösterilemiyor</strong>
                                    <p>
                                        {job.testReportMessage ||
                                            'Test raporu bulunamadı. Test sonuçları bilinmiyor.'}
                                    </p>
                                </div>
                            )}
                        </section>
                        <section className="jd-insight-note">
                            <span>
                                <JenkinsIcon name="info" size={18} />
                            </span>
                            <div>
                                <h3>Build sonucu ve uygulama sağlığı</h3>
                                <p>
                                    Başarılı bir pipeline, uygulamanın şu anda sağlıklı olduğunu tek
                                    başına göstermez. Çalışma durumu Docker ve uygulama sağlığı
                                    bölümünden izlenir.
                                </p>
                            </div>
                        </section>
                        <div className="jd-data-note">
                            <JenkinsIcon name="clock" size={14} />
                            <span>
                                Son başarılı veri: {formatDate(job.fetchedAt)} TRT
                                <br />
                                Güncel durumu görmek için Yenile'yi kullanın.
                            </span>
                        </div>
                    </div>
                </div>
            </>
        )
    }

    return (
        <div className="jd-app jd-integrated">
            <div className="jd-main">
                <nav className="jd-page-breadcrumb" aria-label="Jenkins içerik yolu">
                    <Link to={`/projects/${projectId}`}>← Projeye dön</Link>
                    {project && <span>{project.name} / Jenkins</span>}
                </nav>
                <header className="jd-page-heading">
                    <div className="jd-heading-title">
                        <span className="jd-jenkins-symbol">
                            <JenkinsIcon name="pipeline" size={27} />
                        </span>
                        <div>
                            <p className="jd-overline">PROJE ENTEGRASYONU</p>
                            <h1>Jenkins</h1>
                        </div>
                    </div>
                    <div className="jd-heading-actions">
                        <button
                            className="jd-button jd-button-secondary"
                            onClick={refresh}
                            disabled={isLoading}
                        >
                            <JenkinsIcon name="refresh" size={16} />
                            Yenile
                        </button>
                        {job && (
                            <a
                                className="jd-button jd-button-primary"
                                href={job.jobUrl}
                                target="_blank"
                                rel="noreferrer"
                            >
                                Jenkins'te aç
                                <JenkinsIcon name="arrow" size={16} />
                            </a>
                        )}
                    </div>
                    <p className="jd-heading-description">
                        {project
                            ? `${project.name} projesinin pipeline ve build ayrıntıları.`
                            : 'Pipeline ve build ayrıntıları.'}
                    </p>
                </header>
                {isLoading && (
                    <p className="jd-loading" role="status">
                        Jenkins bilgileri yükleniyor…
                    </p>
                )}
                {error && (
                    <section className="jd-card jd-empty">
                        <span className="jd-empty-symbol jd-amber">
                            <JenkinsIcon name="connection" size={30} />
                        </span>
                        <h2>Jenkins ayrıntıları alınamadı</h2>
                        <p role="alert">{error}</p>
                        <small>
                            URL ve job yolunu genel proje profilinden düzenleyebilirsiniz.
                        </small>
                        <Link
                            className="jd-button jd-button-secondary"
                            to={`/projects/${projectId}`}
                        >
                            Proje profiline dön
                        </Link>
                    </section>
                )}
                {job && project && !isLoading && !error && (
                    <>
                        <div className="jd-connection-bar">
                            <div>
                                <JenkinsIcon name="connection" size={15} />
                                <span className="jd-connection-state">
                                    <span />
                                    Jenkins bağlı
                                </span>
                                <span className="jd-connection-divider" />
                                <span className="jd-job-name">{job.jobName}</span>
                                <span className="jd-environment">{project.environment}</span>
                            </div>
                            <span className="jd-last-sync">
                                <JenkinsIcon name="clock" size={13} />
                                Son güncelleme {formatDate(job.fetchedAt)}
                                <span>TRT</span>
                            </span>
                        </div>
                        {renderJobDetails()}
                    </>
                )}
                <footer className="jd-page-footer">
                    <span>
                        OpsBridge<span> / </span>Jenkins görünürlüğü
                    </span>
                    <span>
                        {project?.name || 'Proje'}
                        <span className="jd-footer-dot" />
                        {project?.environment || 'Jenkins'}
                    </span>
                </footer>
            </div>
        </div>
    )
}

export default JenkinsDetailsPage
