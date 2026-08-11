/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_OTEL_EXPORTER_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
