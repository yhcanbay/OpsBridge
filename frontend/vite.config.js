import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const proxy = {
    '/api': { target: 'http://127.0.0.1:8082', changeOrigin: true },
    '/actuator': { target: 'http://127.0.0.1:8082', changeOrigin: true }
}

export default defineConfig({
    plugins: [react()],
    server: { host: '127.0.0.1', port: 5174, strictPort: true, proxy },
    preview: { proxy },
    test: {
        environment: 'jsdom',
        clearMocks: true,
        restoreMocks: true
    }
})
