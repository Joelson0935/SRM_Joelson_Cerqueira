import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Encaminha as chamadas /api para o backend Spring Boot em dev,
    // evitando problemas de CORS. Em produção o frontend é servido
    // atrás do mesmo host/reverse-proxy que o backend.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
