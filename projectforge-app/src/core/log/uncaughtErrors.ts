/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////


import { appLog } from './appLog';

interface HermesRejectionTracker {
  enablePromiseRejectionTracker?: (options: {
    allRejections: boolean;
    onUnhandled: (id: number, error: unknown) => void;
  }) => void;
}

/**
 * Errors nobody caught go into the client log, in addition to the default handling: those of the JavaScript (the
 * red screen of a dev build, the crash of a release build) and, in a release build, promises rejected without a
 * catch (a dev build shows them as warnings of React Native, which keeps its own tracker).
 */
export function logUncaughtErrors() {
  const handler = ErrorUtils.getGlobalHandler();
  ErrorUtils.setGlobalHandler((error, fatal) => {
    appLog.error('app', fatal ? 'Fatal error' : 'Uncaught error', error);
    handler(error, fatal);
  });
  if (!__DEV__) {
    const hermes = (globalThis as { HermesInternal?: HermesRejectionTracker }).HermesInternal;
    hermes?.enablePromiseRejectionTracker?.({
      allRejections: true,
      onUnhandled: (_, error) => appLog.error('app', 'Unhandled rejection', error),
    });
  }
  appLog.info('app', 'Started');
}
