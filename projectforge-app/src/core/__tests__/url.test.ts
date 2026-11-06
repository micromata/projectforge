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

import { parseUrl } from '../url';

// Jest runs on Node, whose global URL is WHATWG conformant: the tests of the url rules would pass with it, while the
// app on the device would use React Native's. This test documents why src/core/url.ts exists.
test("React Native's own URL resolves differently than a WebView, the safe one as a WebView", () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { URL: ReactNativeURL } = require('react-native/Libraries/Blob/URL');
  const base = 'https://pf.example.org';
  // The WebView loads evil.example.org, React Native's URL tells no host at all (or the base's, depending on the form).
  expect(new ReactNativeURL('//evil.example.org/x', base).hostname).not.toBe('evil.example.org');
  expect(parseUrl('//evil.example.org/x', base)?.hostname).toBe('evil.example.org');
  expect(parseUrl('https://pf.example.org\\@evil.example.org/')?.hostname).toBe('pf.example.org');
  expect(parseUrl('https://user@evil.example.org/')?.hostname).toBe('evil.example.org');
});
