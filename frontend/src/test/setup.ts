import '@testing-library/jest-dom/vitest'
import { expect } from 'vitest'
import * as matchers from 'vitest-axe/matchers'

expect.extend(matchers)

// jsdom lacks canvas; axe color-contrast probes call getContext.
HTMLCanvasElement.prototype.getContext = (() => null) as typeof HTMLCanvasElement.prototype.getContext

