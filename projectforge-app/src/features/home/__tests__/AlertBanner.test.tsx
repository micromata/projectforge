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


// Port of projectforge-flutter/test/features/home/system_alert_banner_test.dart.
import { render, screen } from '@testing-library/react-native';

import { AlertBanner } from '../AlertBanner';

const mockStatus: { current: { alertMessage?: string } | null } = { current: null };
jest.mock('../../../core/status/userStatus', () => ({
  ...jest.requireActual('../../../core/status/userStatus'),
  useUserStatus: () => ({ status: mockStatus.current, theme: 'system' }),
}));

test('shows the message of the administrators', async () => {
  mockStatus.current = { alertMessage: ' Maintenance at 18:00 ' };
  await render(<AlertBanner />);
  expect(screen.getByText(/Maintenance at 18:00/)).toBeTruthy();
});

test('shows nothing without a message', async () => {
  mockStatus.current = { alertMessage: ' ' };
  await render(<AlertBanner />);
  expect(screen.toJSON()).toBeNull();
});
