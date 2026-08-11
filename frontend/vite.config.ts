import fs from 'node:fs'
import path from 'node:path'
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

/** Local dev BFF stubs until api-gateway auth routes are wired (WO-224). */
function devAuthMockPlugin(): Plugin {
  const sessionPath = path.resolve(__dirname, 'fixtures/auth/session-csr.json')
  return {
    name: 'pcis-dev-auth-mock',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url === '/api/auth/session' && req.method === 'GET') {
          res.setHeader('Content-Type', 'application/json')
          res.end(fs.readFileSync(sessionPath, 'utf8'))
          return
        }
        if (req.url === '/api/auth/callback' && req.method === 'POST') {
          res.statusCode = 204
          res.end()
          return
        }
        if (req.url === '/api/auth/logout' && req.method === 'POST') {
          res.statusCode = 204
          res.end()
          return
        }
        next()
      })
    },
  }
}

export default defineConfig({
  plugins: [react(), devAuthMockPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
})
