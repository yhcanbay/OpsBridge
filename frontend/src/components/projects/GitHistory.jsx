import { useState } from 'react'
import { useGitHistory } from '../../hooks/useGitHistory.js'

function GitHistory({ projectId }) {
    const [query, setQuery] = useState({ branch: '', page: 1, branchPage: 1 })
    const { history, isLoading, error } = useGitHistory(projectId, query)

    function handleRefresh() {
        setQuery({ branch: '', page: 1, branchPage: 1 })
    }

    function handleBranchChange(event) {
        setQuery({ branch: event.target.value, page: 1, branchPage: history.branchPage })
    }

    function handleCommitPageChange(page) {
        setQuery({ branch: history.selectedBranch, page, branchPage: history.branchPage })
    }

    function handleBranchPageChange(branchPage) {
        setQuery({ branch: '', page: 1, branchPage })
    }

    return (
        <section className="detail-section git-history" aria-labelledby="git-history-title">
            <div className="section-heading">
                <h3 id="git-history-title">GitHub geçmişi</h3>
                <button className="button secondary" onClick={handleRefresh} disabled={isLoading}>
                    Yenile
                </button>
            </div>
            <p className="small muted">
                GitHub üzerindeki branch ve commit bilgileri. Çalışan Docker sürümünün commit
                bilgisi henüz okunmuyor.
            </p>
            {isLoading && <p role="status">GitHub geçmişi yükleniyor…</p>}
            {error && <p role="alert">{error}</p>}
            {history && (
                <>
                    {history.branches.length > 0 ? (
                        <div className="git-branch-controls">
                            <label htmlFor="git-branch">Branch</label>
                            <select
                                id="git-branch"
                                value={history.selectedBranch}
                                onChange={handleBranchChange}
                            >
                                {history.branches.map((branch) => (
                                    <option key={branch} value={branch}>
                                        {branch}
                                    </option>
                                ))}
                            </select>
                        </div>
                    ) : (
                        <p>Bu sayfada branch bulunamadı.</p>
                    )}
                    <div className="git-pagination" aria-label="Branch sayfaları">
                        <button
                            className="text-button"
                            disabled={history.branchPage === 1}
                            onClick={() => handleBranchPageChange(history.branchPage - 1)}
                        >
                            Önceki branch’ler
                        </button>
                        <span className="small muted">Branch sayfası {history.branchPage}</span>
                        <button
                            className="text-button"
                            disabled={!history.hasMoreBranches}
                            onClick={() => handleBranchPageChange(history.branchPage + 1)}
                        >
                            Sonraki branch’ler
                        </button>
                    </div>
                    {history.commits.length > 0 ? (
                        <ol className="git-commits" aria-label="Commit geçmişi">
                            {history.commits.map((commit) => (
                                <li key={commit.sha}>
                                    <code title={commit.sha}>{commit.sha.slice(0, 7)}</code>
                                    <p className="git-commit-message">{commit.message}</p>
                                    <span className="small muted">
                                        {commit.author} ·{' '}
                                        {commit.committedAt
                                            ? new Date(commit.committedAt).toLocaleString('tr-TR')
                                            : 'Tarih bilinmiyor'}
                                    </span>
                                </li>
                            ))}
                        </ol>
                    ) : (
                        <p>Bu sayfada commit bulunamadı.</p>
                    )}
                    {history.selectedBranch && (
                        <div className="git-pagination" aria-label="Commit sayfaları">
                            <button
                                className="text-button"
                                disabled={history.page === 1}
                                onClick={() => handleCommitPageChange(history.page - 1)}
                            >
                                Önceki commit’ler
                            </button>
                            <span className="small muted">Commit sayfası {history.page}</span>
                            <button
                                className="text-button"
                                disabled={!history.hasMoreCommits}
                                onClick={() => handleCommitPageChange(history.page + 1)}
                            >
                                Sonraki commit’ler
                            </button>
                        </div>
                    )}
                    <p className="small muted">
                        Son başarılı okuma · {new Date(history.fetchedAt).toLocaleString('tr-TR')}
                    </p>
                </>
            )}
        </section>
    )
}

export default GitHistory
