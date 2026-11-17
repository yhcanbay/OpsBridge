import { useRef, useState } from 'react'
import JenkinsIcon from '../components/jenkins/JenkinsIcon.jsx'
import '../pages/JenkinsDetailsPage.css'

// Yalnızca tasarım verileri. Bu sayfa HTTP isteği göndermez.
const exampleBuildHistory = [
    { number: 47, status: 'failed', time: '14:24', duration: 72, commit: 'a8f3c21' },
    { number: 46, status: 'success', time: '14:10', duration: 128, commit: '7d2e9b4' },
    { number: 45, status: 'success', time: '13:45', duration: 123, commit: '6c1a8f3' },
    { number: 44, status: 'success', time: '13:20', duration: 140, commit: '5b4d2e1' },
    { number: 43, status: 'success', time: '12:52', duration: 115, commit: '4a3c9d2' },
    { number: 42, status: 'failed', time: '12:30', duration: 96, commit: '3e7b6a1' },
    { number: 41, status: 'success', time: '12:05', duration: 132, commit: '2d6f5c8' },
    { number: 40, status: 'success', time: '11:40', duration: 123, commit: '1c5e4b7' },
    { number: 39, status: 'success', time: '11:15', duration: 121, commit: '0b4d3a6' },
    { number: 38, status: 'success', time: '10:50', duration: 130, commit: '9a3c2f5' }
]

const stageNames = [
    'Kodu alma',
    'Backend testleri',
    'Frontend derleme',
    'Docker deploy',
    'Doğrulama'
]

const statusLabels = {
    running: 'Devam ediyor',
    success: 'Başarılı',
    failed: 'Başarısız',
    waiting: 'Bekliyor',
    skipped: 'Atlandı'
}

function BuildStatus({ status }) {
    let icon = 'clock'

    if (status === 'success') icon = 'check'
    if (status === 'failed') icon = 'close'

    return (
        <span className={`jd-status jd-${status}`}>
            <JenkinsIcon name={icon} size={13} />
            {statusLabels[status]}
        </span>
    )
}

function formatDuration(seconds) {
    if (seconds < 60) return `${seconds} sn`

    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = String(seconds % 60).padStart(2, '0')
    return `${minutes} dk ${remainingSeconds} sn`
}

function JenkinsDesignPreview() {
    const [scenario, setScenario] = useState('running')
    const [selectedBuildNumber, setSelectedBuildNumber] = useState(null)
    const [historyFilter, setHistoryFilter] = useState('all')
    const [updatedAt, setUpdatedAt] = useState('14:33:02')
    const [notice, setNotice] = useState('')
    const [resource, setResource] = useState({ title: '', path: '' })
    const resourceDialog = useRef(null)

    const isUnavailable = ['offline', 'missing-job', 'empty'].includes(scenario)
    let buildHistory = exampleBuildHistory

    if (scenario === 'success' || scenario === 'no-report') {
        const completedBuild = {
            number: 48,
            status: 'success',
            time: '14:30',
            duration: 128,
            commit: 'a8f3c21'
        }
        buildHistory = [completedBuild, ...exampleBuildHistory].slice(0, 10)
    }

    const lastCompletedBuild = buildHistory[0]
    const lastSuccessfulBuild = buildHistory.find((build) => build.status === 'success')
    const selectedBuild = buildHistory.find((build) => build.number === selectedBuildNumber)
    let currentStatus = 'running'

    if (scenario === 'failed') currentStatus = 'failed'
    if (scenario === 'success' || scenario === 'no-report') currentStatus = 'success'
    if (selectedBuild) currentStatus = selectedBuild.status

    const buildNumber = selectedBuild?.number ?? (currentStatus === 'failed' ? 47 : 48)
    const buildCommit = selectedBuild?.commit ?? 'a8f3c21'
    let startTime = '14:32'

    if (currentStatus === 'failed') startTime = '14:24'
    if (currentStatus === 'success') startTime = '14:30'
    if (selectedBuild) startTime = selectedBuild.time
    const buildDuration =
        selectedBuild?.duration ??
        (currentStatus === 'running' ? 54 : currentStatus === 'failed' ? 72 : 128)
    const buildPath = `/job/personal-task-app/${buildNumber}/`
    const visibleHistory = buildHistory.filter(
        (build) => historyFilter === 'all' || build.status === historyFilter
    )
    const successfulBuildCount = buildHistory.filter((build) => build.status === 'success').length
    const successRate = Math.round((successfulBuildCount / buildHistory.length) * 100)
    const averageDuration = Math.round(
        buildHistory.reduce((total, build) => total + build.duration, 0) / buildHistory.length
    )
    let stageStatuses = ['success', 'success', 'running', 'waiting', 'waiting']
    let stageDurations = [4, 32, 18, null, null]

    if (currentStatus === 'failed') {
        stageStatuses = ['success', 'failed', 'skipped', 'skipped', 'skipped']
        stageDurations = [4, buildDuration - 4, null, null, null]
    }

    if (currentStatus === 'success') {
        stageStatuses = ['success', 'success', 'success', 'success', 'success']
        stageDurations = [4, 32, 28, buildDuration - 73, 9]
    }

    function handleScenarioChange(event) {
        setScenario(event.target.value)
        setSelectedBuildNumber(null)
        setHistoryFilter('all')
        setNotice('')
        setUpdatedAt('14:33:02')
    }

    function handleRefresh() {
        const time = new Intl.DateTimeFormat('tr-TR', {
            timeZone: 'Europe/Istanbul',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        }).format(new Date())

        setUpdatedAt(time)
        setNotice('Örnek görünüm yenilendi. Canlı Jenkins bağlantısı kullanılmıyor.')
    }

    function handleBuildSelect(number) {
        setSelectedBuildNumber(number)
        setNotice(`#${number} numaralı örnek build'in ayrıntıları gösteriliyor.`)
    }

    function handleCurrentBuildSelect() {
        setSelectedBuildNumber(null)
        setNotice('Son build gösteriliyor.')
    }

    function handleHistoryFilterChange(event) {
        setHistoryFilter(event.target.value)
    }

    function handleOpenResource(title, path) {
        setResource({ title, path })
        resourceDialog.current.showModal()
    }

    function handleDialogClose() {
        resourceDialog.current.close()
    }

    function handleDialogBackdropClick(event) {
        if (event.target === event.currentTarget) handleDialogClose()
    }

    function renderUnavailableState() {
        const messages = {
            offline: {
                icon: 'connection',
                title: "Jenkins'e ulaşılamıyor",
                description:
                    'Bağlantı yeniden kurulana kadar güncel build ve aşama bilgileri gösterilemiyor.',
                detail: 'Son başarılı erişim: 14 Kasım 2026, 14:31:02 · Build sonucu bilinmiyor.'
            },
            'missing-job': {
                icon: 'search',
                title: 'Jenkins job bulunamadı',
                description:
                    'Jenkins erişilebilir, ancak tanımlanan personal-task-app job yolu bulunamadı.',
                detail: 'Proje profilindeki job yolunun Jenkins üzerindeki adıyla eşleşmesi gerekir.'
            },
            empty: {
                icon: 'pipeline',
                title: 'İlk build bekleniyor',
                description:
                    'Job bağlantısı hazır. Henüz tamamlanmış veya devam eden bir çalışma yok.',
                detail: 'İlk pipeline çalıştığında aşamalar, commit bilgisi ve build geçmişi burada görünecek.'
            }
        }
        const message = messages[scenario]

        return (
            <section className="jd-card jd-empty" aria-labelledby="jd-empty-title">
                <span className={`jd-empty-symbol ${scenario === 'offline' ? 'jd-amber' : ''}`}>
                    <JenkinsIcon name={message.icon} size={30} />
                </span>
                <span className="jd-overline">JENKINS BAĞLANTISI</span>
                <h2 id="jd-empty-title">{message.title}</h2>
                <p>{message.description}</p>
                <small>{message.detail}</small>
                <button className="jd-button jd-button-secondary" onClick={handleRefresh}>
                    <JenkinsIcon name="refresh" size={16} /> Görünümü yenile
                </button>
            </section>
        )
    }

    return (
        <div className="jd-app">
            <a className="jd-skip-link" href="#jd-main">
                İçeriğe geç
            </a>
            <aside className="jd-sidebar" aria-label="Çalışma alanı">
                <div className="jd-brand">
                    <span>ob</span>OpsBridge<span className="jd-brand-dot">.</span>
                </div>
                <div className="jd-workspace-label">ÇALIŞMA ALANI</div>
                <div className="jd-nav-item">
                    <JenkinsIcon name="grid" />
                    Projeler
                    <span className="jd-nav-dot" />
                </div>
                <div className="jd-sidebar-project">
                    <span className="jd-sidebar-line" />
                    <div>
                        <span>Personal Task App</span>
                        <strong>Jenkins ayrıntıları</strong>
                    </div>
                </div>
                <div className="jd-sidebar-bottom">
                    <div className="jd-sidebar-caption">
                        <JenkinsIcon name="shield" size={17} />
                        <span>Operasyonlara tek bakış.</span>
                    </div>
                    <div className="jd-account">
                        <span className="jd-avatar">A</span>
                        <div>
                            <strong>admin</strong>
                            <small>Çalışma alanı yöneticisi</small>
                        </div>
                        <span className="jd-account-dot" />
                    </div>
                </div>
            </aside>

            <div className="jd-workspace">
                <header className="jd-topbar">
                    <nav aria-label="İçerik yolu">
                        <ol>
                            <li>Projeler</li>
                            <li>
                                <JenkinsIcon name="chevron" size={12} />
                                Personal Task App
                            </li>
                            <li>
                                <JenkinsIcon name="chevron" size={12} />
                                <strong>Jenkins</strong>
                            </li>
                        </ol>
                    </nav>
                    <span className="jd-local-label">
                        <span />
                        Yerel geliştirme
                    </span>
                </header>

                <main id="jd-main" className="jd-main">
                    <div className="jd-preview-bar">
                        <span>
                            <JenkinsIcon name="info" size={15} />
                            <strong>Tasarım önizlemesi</strong>
                            <span className="jd-preview-description">
                                Örnek veriler · 14 Kasım 2026
                            </span>
                        </span>
                        <label>
                            Görünüm
                            <select
                                value={scenario}
                                onChange={handleScenarioChange}
                                aria-label="Önizleme senaryosu"
                            >
                                <option value="running">Devam eden build</option>
                                <option value="failed">Başarısız build</option>
                                <option value="success">Başarılı build</option>
                                <option value="no-report">Test raporu yok</option>
                                <option value="offline">Jenkins'e ulaşılamıyor</option>
                                <option value="missing-job">Job bulunamadı</option>
                                <option value="empty">Henüz build yok</option>
                            </select>
                        </label>
                    </div>

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
                                onClick={handleRefresh}
                            >
                                <JenkinsIcon name="refresh" size={16} />
                                Yenile
                            </button>
                            <button
                                className="jd-button jd-button-primary"
                                onClick={() =>
                                    handleOpenResource(
                                        "Jenkins'te aç",
                                        'https://jenkins.example.com/job/personal-task-app/'
                                    )
                                }
                            >
                                Jenkins'te aç
                                <JenkinsIcon name="arrow" size={16} />
                            </button>
                        </div>
                        <p className="jd-heading-description">
                            Personal Task App projesinin pipeline ve build ayrıntıları.
                        </p>
                    </header>

                    <div className="jd-connection-bar">
                        <div>
                            <JenkinsIcon name="connection" size={15} />
                            <span
                                className={`jd-connection-state ${scenario === 'offline' ? 'jd-connection-warning' : ''}`}
                            >
                                <span />
                                {scenario === 'offline' ? 'Erişim hatası' : 'Jenkins bağlı'}
                            </span>
                            <span className="jd-connection-divider" />
                            <span className="jd-job-name">personal-task-app</span>
                            <span className="jd-environment">Geliştirme</span>
                        </div>
                        <span className="jd-last-sync">
                            <JenkinsIcon name="clock" size={13} />
                            {scenario === 'offline'
                                ? 'Son başarılı erişim 14:31:02'
                                : `Son güncelleme ${updatedAt}`}
                            <span>TRT</span>
                        </span>
                    </div>

                    <p className="jd-announcement" role="status" aria-live="polite">
                        {notice}
                    </p>

                    {isUnavailable ? (
                        renderUnavailableState()
                    ) : (
                        <>
                            <section
                                className={`jd-build-overview jd-overview-${currentStatus}`}
                                aria-labelledby="jd-build-heading"
                            >
                                <div className="jd-build-headline">
                                    <span className={`jd-build-symbol jd-${currentStatus}`}>
                                        <JenkinsIcon
                                            name={
                                                currentStatus === 'running'
                                                    ? 'play'
                                                    : currentStatus === 'failed'
                                                      ? 'close'
                                                      : 'check'
                                            }
                                            size={23}
                                        />
                                    </span>
                                    <div>
                                        <div className="jd-build-eyebrow">
                                            {selectedBuild ? 'SEÇİLİ ÇALIŞMA' : 'SON ÇALIŞMA'}
                                            <span>#{buildNumber}</span>
                                            <BuildStatus status={currentStatus} />
                                        </div>
                                        <h2 id="jd-build-heading">
                                            {currentStatus === 'running'
                                                ? 'Pipeline çalışıyor.'
                                                : currentStatus === 'failed'
                                                  ? 'Pipeline test aşamasında durdu.'
                                                  : 'Pipeline başarıyla tamamlandı.'}
                                        </h2>
                                        <p>
                                            {currentStatus === 'running'
                                                ? 'Frontend derleniyor. Sonraki adım Docker deploy.'
                                                : currentStatus === 'failed'
                                                  ? 'Backend testleri başarısız. Dağıtım aşaması başlamadı.'
                                                  : 'Tüm aşamalar tamamlandı. Dağıtım sonrası doğrulama başarılı.'}
                                        </p>
                                    </div>
                                </div>
                                <div className="jd-build-timing">
                                    <div>
                                        <span>
                                            {currentStatus === 'running'
                                                ? 'Geçen süre'
                                                : 'Toplam süre'}
                                        </span>
                                        <strong>{formatDuration(buildDuration)}</strong>
                                    </div>
                                    <div>
                                        <span>Başlangıç</span>
                                        <strong>
                                            {startTime}
                                            <small>bugün</small>
                                        </strong>
                                    </div>
                                </div>
                                {selectedBuild && (
                                    <button
                                        className="jd-return-current"
                                        onClick={handleCurrentBuildSelect}
                                    >
                                        Son build'e dön
                                        <JenkinsIcon name="chevron" size={13} />
                                    </button>
                                )}
                            </section>

                            <div className="jd-metrics" aria-label="Son tamamlanmış build özeti">
                                <article>
                                    <span className="jd-metric-label">
                                        Son tamamlanan build
                                        <JenkinsIcon name="pipeline" size={16} />
                                    </span>
                                    <div className="jd-metric-value">
                                        #{lastCompletedBuild.number}
                                        <BuildStatus status={lastCompletedBuild.status} />
                                    </div>
                                    <small>
                                        {lastCompletedBuild.status === 'failed'
                                            ? 'Backend testleri'
                                            : 'Tüm aşamalar tamamlandı'}
                                        {' · bugün '}
                                        {lastCompletedBuild.time}
                                    </small>
                                </article>
                                <article>
                                    <span className="jd-metric-label">
                                        Son başarılı build
                                        <JenkinsIcon name="check" size={17} />
                                    </span>
                                    <div className="jd-metric-value">
                                        #{lastSuccessfulBuild.number}
                                        <span className="jd-metric-subvalue">
                                            {formatDuration(lastSuccessfulBuild.duration)}
                                        </span>
                                    </div>
                                    <small>Bugün {lastSuccessfulBuild.time} · main</small>
                                </article>
                                <article>
                                    <span className="jd-metric-label">
                                        Başarı oranı
                                        <span className="jd-metric-scope">10 BUILD</span>
                                    </span>
                                    <div className="jd-metric-value">
                                        %{successRate}
                                        <span className="jd-metric-subvalue">
                                            {successfulBuildCount} / {buildHistory.length} başarılı
                                        </span>
                                    </div>
                                    <div
                                        className="jd-mini-history"
                                        aria-label="Son 10 tamamlanmış build, eskiden yeniye"
                                    >
                                        {[...buildHistory].reverse().map((build) => (
                                            <span
                                                key={build.number}
                                                className={`jd-${build.status}`}
                                                title={`#${build.number} · ${statusLabels[build.status]}`}
                                            />
                                        ))}
                                    </div>
                                    <small>Son 10 tamamlanmış build</small>
                                </article>
                                <article>
                                    <span className="jd-metric-label">
                                        Ortalama build süresi
                                        <JenkinsIcon name="clock" size={16} />
                                    </span>
                                    <div className="jd-metric-value jd-metric-duration">
                                        {formatDuration(averageDuration)}
                                    </div>
                                    <small>Son 10 tamamlanmış build</small>
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
                                                Build #{buildNumber}
                                                <span>·</span>
                                                <JenkinsIcon name="branch" size={13} />
                                                main
                                            </p>
                                        </div>
                                        <span className="jd-stage-count">
                                            {
                                                stageStatuses.filter(
                                                    (status) => status === 'success'
                                                ).length
                                            }{' '}
                                            / 5 tamamlandı
                                        </span>
                                    </header>
                                    <ol className="jd-stages">
                                        {stageNames.map((name, index) => (
                                            <li
                                                key={name}
                                                className={`jd-stage jd-${stageStatuses[index]}`}
                                            >
                                                <span className="jd-stage-node">
                                                    {stageStatuses[index] === 'success' ? (
                                                        <JenkinsIcon name="check" size={17} />
                                                    ) : stageStatuses[index] === 'failed' ? (
                                                        <JenkinsIcon name="close" size={17} />
                                                    ) : stageStatuses[index] === 'running' ? (
                                                        <span className="jd-stage-spinner" />
                                                    ) : (
                                                        <span className="jd-stage-index">
                                                            {String(index + 1).padStart(2, '0')}
                                                        </span>
                                                    )}
                                                </span>
                                                <strong>{name}</strong>
                                                <span className="jd-stage-status">
                                                    {stageStatuses[index] === 'success'
                                                        ? 'Tamamlandı'
                                                        : statusLabels[stageStatuses[index]]}
                                                </span>
                                                <small>
                                                    {stageDurations[index] === null
                                                        ? '—'
                                                        : formatDuration(stageDurations[index])}
                                                </small>
                                            </li>
                                        ))}
                                    </ol>
                                    <div className={`jd-stage-message jd-message-${currentStatus}`}>
                                        <JenkinsIcon
                                            name={
                                                currentStatus === 'failed'
                                                    ? 'alert'
                                                    : currentStatus === 'success'
                                                      ? 'check'
                                                      : 'info'
                                            }
                                            size={17}
                                        />
                                        <div>
                                            <strong>
                                                {currentStatus === 'running'
                                                    ? 'Frontend derleme aşaması devam ediyor'
                                                    : currentStatus === 'failed'
                                                      ? 'Hata noktası: Backend testleri'
                                                      : '5 aşamanın tamamı başarılı'}
                                            </strong>
                                            <p>
                                                {currentStatus === 'running'
                                                    ? 'Docker deploy ve doğrulama henüz başlamadı.'
                                                    : currentStatus === 'failed'
                                                      ? '3 test başarısız. Sonraki aşamalar çalıştırılmadı.'
                                                      : 'Build, dağıtım ve doğrulama adımları tamamlandı.'}
                                            </p>
                                        </div>
                                    </div>
                                    <footer className="jd-pipeline-footer">
                                        <span>
                                            <JenkinsIcon name="terminal" size={16} />
                                            Ayrıntılı çıktı Jenkins konsolunda
                                        </span>
                                        <button
                                            className="jd-text-button"
                                            onClick={() =>
                                                handleOpenResource(
                                                    'Konsol çıktısı',
                                                    `https://jenkins.example.com${buildPath}console`
                                                )
                                            }
                                        >
                                            Konsolu aç
                                            <JenkinsIcon name="arrow" size={14} />
                                        </button>
                                    </footer>
                                </section>

                                <section
                                    className="jd-card jd-source-card"
                                    aria-labelledby="jd-source-heading"
                                >
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
                                            <dd>Personal_Task_App</dd>
                                        </div>
                                        <div>
                                            <dt>Branch</dt>
                                            <dd>
                                                <span className="jd-branch-pill">
                                                    <JenkinsIcon name="branch" size={12} />
                                                    main
                                                </span>
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Build commit'i</dt>
                                            <dd>
                                                <button
                                                    className="jd-commit-button"
                                                    onClick={() =>
                                                        handleOpenResource(
                                                            'GitHub commit ayrıntısı',
                                                            `https://github.com/example/Personal_Task_App/commit/${buildCommit}`
                                                        )
                                                    }
                                                >
                                                    <JenkinsIcon name="commit" size={14} />
                                                    {buildCommit}
                                                    <JenkinsIcon name="arrow" size={12} />
                                                </button>
                                            </dd>
                                        </div>
                                    </dl>
                                    <div className="jd-commit-message">
                                        <span className="jd-commit-track" />
                                        <div>
                                            <strong>Görev yönetimi güncellemeleri</strong>
                                            <span>1 commit · bu build'e dahil</span>
                                        </div>
                                    </div>
                                    <div className="jd-version-notes">
                                        <div>
                                            <span>Branch'in son commit'i</span>
                                            <code>b9e4d72</code>
                                        </div>
                                        <div>
                                            <span>Çalışan uygulamanın sürümü</span>
                                            <strong>Bilinmiyor</strong>
                                        </div>
                                        <p>
                                            <JenkinsIcon name="info" size={13} />
                                            Çalışan sürüm için Docker eşleştirmesi gerekir.
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
                                                <span className="jd-count-badge">10</span>
                                            </h2>
                                            <p>Son tamamlanmış çalışmalar</p>
                                        </div>
                                        <label className="jd-history-filter">
                                            <span className="jd-sr-only">
                                                Build sonucuna göre filtrele
                                            </span>
                                            <select
                                                value={historyFilter}
                                                onChange={handleHistoryFilterChange}
                                            >
                                                <option value="all">Tüm sonuçlar</option>
                                                <option value="success">Başarılı</option>
                                                <option value="failed">Başarısız</option>
                                            </select>
                                        </label>
                                    </header>
                                    <div className="jd-table-scroll">
                                        <table className="jd-build-table">
                                            <caption className="jd-sr-only">
                                                14 Kasım 2026 tarihli son 10 tamamlanmış build.
                                                Build numarasını seçerek ayrıntılarını
                                                görebilirsiniz.
                                            </caption>
                                            <thead>
                                                <tr>
                                                    <th scope="col">Build</th>
                                                    <th scope="col">Sonuç</th>
                                                    <th scope="col">Commit</th>
                                                    <th scope="col">Başlangıç</th>
                                                    <th scope="col">Süre</th>
                                                    <th scope="col">
                                                        <span className="jd-sr-only">
                                                            Jenkins bağlantısı
                                                        </span>
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {visibleHistory.map((build) => (
                                                    <tr
                                                        key={build.number}
                                                        className={
                                                            selectedBuildNumber === build.number
                                                                ? 'jd-selected-row'
                                                                : ''
                                                        }
                                                    >
                                                        <td>
                                                            <button
                                                                className="jd-build-number"
                                                                aria-pressed={
                                                                    selectedBuildNumber ===
                                                                    build.number
                                                                }
                                                                onClick={() =>
                                                                    handleBuildSelect(build.number)
                                                                }
                                                            >
                                                                #{build.number}
                                                            </button>
                                                        </td>
                                                        <td>
                                                            <BuildStatus status={build.status} />
                                                        </td>
                                                        <td>
                                                            <code>{build.commit}</code>
                                                        </td>
                                                        <td>
                                                            {build.time}
                                                            <span className="jd-table-today">
                                                                bugün
                                                            </span>
                                                        </td>
                                                        <td>{formatDuration(build.duration)}</td>
                                                        <td>
                                                            <button
                                                                className="jd-icon-button"
                                                                aria-label={`Build #${build.number} Jenkins ayrıntıları`}
                                                                onClick={() =>
                                                                    handleOpenResource(
                                                                        `Build #${build.number}`,
                                                                        `https://jenkins.example.com/job/personal-task-app/${build.number}/`
                                                                    )
                                                                }
                                                            >
                                                                <JenkinsIcon
                                                                    name="arrow"
                                                                    size={16}
                                                                />
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                    <footer className="jd-table-footer">
                                        <span>{visibleHistory.length} çalışma gösteriliyor</span>
                                        <span>
                                            {scenario === 'running'
                                                ? 'Devam eden build bu listeye dahil değil.'
                                                : 'En yeni çalışma ilk sırada.'}
                                        </span>
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
                                                <p>Build #{buildNumber} · Backend testleri</p>
                                            </div>
                                        </header>
                                        {scenario === 'no-report' && !selectedBuild ? (
                                            <div className="jd-no-report">
                                                <JenkinsIcon name="report" size={26} />
                                                <strong>Test raporu bulunamadı</strong>
                                                <p>
                                                    Bu build için yayımlanmış bir rapor yok. Test
                                                    sonuçları bilinmiyor.
                                                </p>
                                            </div>
                                        ) : (
                                            <>
                                                <div className="jd-test-numbers">
                                                    <div>
                                                        <strong>44</strong>
                                                        <span>Toplam test</span>
                                                    </div>
                                                    <div>
                                                        <strong className="jd-test-passed">
                                                            {currentStatus === 'failed' ? 41 : 44}
                                                        </strong>
                                                        <span>Başarılı</span>
                                                    </div>
                                                    <div>
                                                        <strong
                                                            className={
                                                                currentStatus === 'failed'
                                                                    ? 'jd-test-failed'
                                                                    : ''
                                                            }
                                                        >
                                                            {currentStatus === 'failed' ? 3 : 0}
                                                        </strong>
                                                        <span>Başarısız</span>
                                                    </div>
                                                </div>
                                                <div
                                                    className="jd-test-progress"
                                                    role="img"
                                                    aria-label={`${currentStatus === 'failed' ? 41 : 44} başarılı, ${currentStatus === 'failed' ? 3 : 0} başarısız test`}
                                                >
                                                    <span
                                                        style={{
                                                            width: `${currentStatus === 'failed' ? (41 / 44) * 100 : 100}%`
                                                        }}
                                                    />
                                                </div>
                                                {currentStatus === 'failed' ? (
                                                    <div className="jd-failed-test">
                                                        <span>İLK BAŞARISIZ TEST</span>
                                                        <code>TaskServiceTest</code>
                                                        <p>updateTask_shouldSaveChanges</p>
                                                    </div>
                                                ) : (
                                                    <p className="jd-test-note">
                                                        <JenkinsIcon name="check" size={14} />
                                                        Backend testleri tamamlandı.
                                                    </p>
                                                )}
                                                <button
                                                    className="jd-report-link"
                                                    onClick={() =>
                                                        handleOpenResource(
                                                            'Test raporu',
                                                            `https://jenkins.example.com${buildPath}testReport/`
                                                        )
                                                    }
                                                >
                                                    Test raporunu aç
                                                    <JenkinsIcon name="arrow" size={14} />
                                                </button>
                                            </>
                                        )}
                                    </section>
                                    <section className="jd-insight-note">
                                        <span>
                                            <JenkinsIcon name="info" size={18} />
                                        </span>
                                        <div>
                                            <h3>Build sonucu ve uygulama sağlığı</h3>
                                            <p>
                                                Başarılı bir pipeline, uygulamanın şu anda sağlıklı
                                                olduğunu tek başına göstermez. Çalışma durumu Docker
                                                ve uygulama sağlığı bölümünden izlenir.
                                            </p>
                                        </div>
                                    </section>
                                    <div className="jd-data-note">
                                        <JenkinsIcon name="clock" size={14} />
                                        <span>
                                            Veri zamanı: {updatedAt} TRT
                                            <br />
                                            Bu önizlemede canlı veri kullanılmıyor.
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </>
                    )}

                    <footer className="jd-page-footer">
                        <span>
                            OpsBridge<span> / </span>Jenkins görünürlüğü
                        </span>
                        <span>
                            Personal Task App
                            <span className="jd-footer-dot" />
                            Geliştirme
                        </span>
                    </footer>
                </main>
            </div>

            <dialog
                ref={resourceDialog}
                className="jd-resource-dialog"
                aria-labelledby="jd-resource-title"
                onClick={handleDialogBackdropClick}
            >
                <div className="jd-dialog-top">
                    <span className="jd-overline">BAĞLANTI ÖNİZLEMESİ</span>
                    <button
                        className="jd-icon-button"
                        aria-label="Pencereyi kapat"
                        onClick={handleDialogClose}
                    >
                        <JenkinsIcon name="close" />
                    </button>
                </div>
                <span className="jd-dialog-icon">
                    <JenkinsIcon name="arrow" size={26} />
                </span>
                <h2 id="jd-resource-title">{resource.title}</h2>
                <p>Entegrasyon tamamlandığında ilgili ayrıntı bu bağlantıda açılacak.</p>
                <code>{resource.path}</code>
                <p className="jd-dialog-footnote">
                    Örnek adres gösteriliyor. Bu tasarım canlı Jenkins veya GitHub bağlantısı açmaz.
                </p>
                <button className="jd-button jd-button-primary" onClick={handleDialogClose}>
                    Önizlemeye dön
                </button>
            </dialog>
        </div>
    )
}

export default JenkinsDesignPreview
