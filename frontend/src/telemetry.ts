import { WebTracerProvider } from '@opentelemetry/sdk-trace-web'
import { SimpleSpanProcessor } from '@opentelemetry/sdk-trace-base'
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http'
import { Resource } from '@opentelemetry/resources'
import { SEMRESATTRS_SERVICE_NAME } from '@opentelemetry/semantic-conventions'
import { registerInstrumentations } from '@opentelemetry/instrumentation'
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch'

const CORRELATION_HEADER = 'X-Correlation-ID'

function createCorrelationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `corr-${Date.now()}`
}

/**
 * Ensures every fetch call carries X-Correlation-ID for distributed tracing.
 */
function installCorrelationHeaderPatch(): void {
  if (typeof window === 'undefined' || typeof window.fetch !== 'function') {
    return
  }

  const originalFetch = window.fetch.bind(window)
  window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const correlationId = createCorrelationId()
    const headers = new Headers(init?.headers ?? (input instanceof Request ? input.headers : undefined))
    if (!headers.has(CORRELATION_HEADER)) {
      headers.set(CORRELATION_HEADER, correlationId)
    }
    return originalFetch(input, { ...init, headers })
  }
}

/**
 * Initializes OpenTelemetry browser tracing with fetch instrumentation.
 * Failures are logged and swallowed so a missing collector never crashes the SPA.
 */
export function initTelemetry(): void {
  try {
    installCorrelationHeaderPatch()

    const exporter = new OTLPTraceExporter({
      url: import.meta.env.VITE_OTEL_EXPORTER_URL ?? 'http://localhost:4318/v1/traces',
    })

    const provider = new WebTracerProvider({
      resource: new Resource({
        [SEMRESATTRS_SERVICE_NAME]: 'pcis-frontend',
      }),
    })

    provider.addSpanProcessor(new SimpleSpanProcessor(exporter))
    provider.register()

    registerInstrumentations({
      instrumentations: [
        new FetchInstrumentation({
          propagateTraceHeaderCorsUrls: [/.*/],
          clearTimingResources: true,
        }),
      ],
    })
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : String(error)
    console.warn('[telemetry] OpenTelemetry init failed; continuing without tracing', {
      message,
    })
  }
}
