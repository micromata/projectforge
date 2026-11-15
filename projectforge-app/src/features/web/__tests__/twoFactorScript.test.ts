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

import { twoFactorAnswerScript } from '../twoFactorScript';

/** Runs the script in a page; the events it dispatched. */
function run(ok: boolean) {
  const events: { type: string; detail: unknown }[] = [];
  const window = { dispatchEvent: (event: { type: string; detail: unknown }) => events.push(event) };
  const CustomEvent = class {
    detail: unknown;
    constructor(
      public type: string,
      init: { detail: unknown },
    ) {
      this.detail = init.detail;
    }
  };
  new Function('window', 'CustomEvent', twoFactorAnswerScript(ok))(window, CustomEvent);
  return events;
}

test('the page is told whether the second factor was confirmed', () => {
  expect(run(true)).toEqual([{ type: 'pf-app-2fa', detail: { ok: true } }]);
  expect(run(false)).toEqual([{ type: 'pf-app-2fa', detail: { ok: false } }]);
});
