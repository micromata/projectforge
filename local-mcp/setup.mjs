// Initialize only this demo's loopback instance. Never point this script at a production system.
import { setTimeout as delay } from 'node:timers/promises';
const base = 'http://127.0.0.1:18080/rsPublic/setup';
let ready = false;
for (let attempt = 0; attempt < 300; attempt++) {
  try {
    const response = await fetch(`${base}/status`, { signal: AbortSignal.timeout(2000) });
    if (response.ok) {
      const status = await response.json();
      if (!status.alreadyInitialized) {
        console.log('Creating the local ProjectForge test database…');
        const setup = await fetch(base, { method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ setupTarget: 'TEST_DATA', username: 'admin', password: 'Local-Demo-2026!', passwordRepeat: 'Local-Demo-2026!', timeZone: 'Europe/Berlin', calendarDomain: 'local' }),
          signal: AbortSignal.timeout(300000) });
        const result = await setup.json();
        if (!result.success) throw new Error(`Setup failed: ${JSON.stringify(result)}`);
        console.log('Created test data and development users. Web admin: admin / Local-Demo-2026!');
      }
      ready = true; break;
    }
  } catch (error) {
    if (String(error).includes('Setup failed')) throw error;
  }
  await delay(2000);
}
if (!ready) throw new Error('ProjectForge did not become ready; inspect .local/mcp/projectforge.log');
