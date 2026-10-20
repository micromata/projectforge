import { z } from "zod";

/**
 * Zod compiles its object parsers with `new Function` unless told not to. The Content Security Policy of
 * ProjectForge has no `'unsafe-eval'` (projectforge.security.csp-header-value), so the browser refuses
 * that and reports a violation on every page; Zod then falls back to the plain parser anyway. Jitless
 * from the start: same result, without the probe and its console error.
 */
z.config({ jitless: true });
