const iconPaths = {
    grid: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M3 14h7v7H3z', 'M14 14h7v7h-7z'],
    pipeline: [
        'M6 8v8',
        'M8 6h8',
        'M18 8v8',
        'M8 18h8',
        'M4 4h4v4H4z',
        'M16 4h4v4h-4z',
        'M4 16h4v4H4z',
        'M16 16h4v4h-4z'
    ],
    arrow: ['M7 17 17 7', 'M7 7h10v10'],
    chevron: ['m9 5 7 7-7 7'],
    refresh: [
        'M20 7v5h-5',
        'M4 17v-5h5',
        'M6.1 6.1A8 8 0 0 1 19.5 9',
        'M4.5 15a8 8 0 0 0 13.4 2.9'
    ],
    check: ['m5 12 4 4L19 6'],
    close: ['m6 6 12 12', 'M6 18 18 6'],
    clock: ['M12 8v5l3 2', 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0'],
    branch: [
        'M6 3v12',
        'M18 9a6 6 0 0 1-6 6H6',
        'M8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0',
        'M20 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0'
    ],
    commit: ['M3 12h5', 'M16 12h5', 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0'],
    code: ['m8 7-5 5 5 5', 'm16 7 5 5-5 5', 'm14 4-4 16'],
    report: ['M14 3H5v18h14V8z', 'M14 3v5h5', 'M8 12h8', 'M8 16h5'],
    info: ['M12 11v6', 'M12 7h.01', 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0'],
    connection: [
        'M8 12a4 4 0 0 1 0-6l3-3a4 4 0 0 1 6 6l-1 1',
        'M16 12a4 4 0 0 1 0 6l-3 3a4 4 0 0 1-6-6l1-1',
        'm9 15 6-6'
    ],
    terminal: ['m5 7 5 5-5 5', 'M13 17h6'],
    alert: ['m12 3 10 18H2z', 'M12 9v5', 'M12 17h.01'],
    play: ['m9 5 10 7-10 7z'],
    search: ['m16 16 5 5', 'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0'],
    shield: ['m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6z', 'm8 12 3 3 5-6']
}

function JenkinsIcon({ name, size = 18, className = '' }) {
    return (
        <svg
            className={`jd-icon ${className}`}
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            {iconPaths[name].map((path, index) => (
                <path key={index} d={path} />
            ))}
        </svg>
    )
}

export default JenkinsIcon
