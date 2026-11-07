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

import { Translations } from '../translations';

test('the app texts lie over the generated ones, placeholders are replaced, a missing key shows itself', () => {
  const translations = new Translations('en', [
    { _generated: 'marker', 'calendar.title': 'Calendar', 'app.version': 'v' },
    { _comment: 'x', 'app.version': 'Version {0}', 'app.pair': '{0} and {1}, again {0}' },
  ]);
  expect(translations.t('calendar.title')).toBe('Calendar');
  expect(translations.t('app.version', '1.1.0')).toBe('Version 1.1.0');
  expect(translations.t('app.pair', 'a', null)).toBe('a and , again a');
  expect(translations.t('missing.key')).toBe('missing.key');
  expect(translations.t('_comment')).toBe('_comment');
});
