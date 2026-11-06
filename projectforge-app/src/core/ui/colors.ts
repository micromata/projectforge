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

import { useColorScheme } from 'react-native';

/** The colors of the app's own screens, light and dark (the theme follows the web pages, see userStatus). */
export interface Colors {
  background: string;
  surface: string;
  text: string;
  textMuted: string;
  border: string;
  primary: string;
  onPrimary: string;
  danger: string;
  warning: string;
  badge: string;
}

const LIGHT: Colors = {
  background: '#ffffff',
  surface: '#f3f4f6',
  text: '#111827',
  textMuted: '#6b7280',
  border: '#d1d5db',
  primary: '#1976d2',
  onPrimary: '#ffffff',
  danger: '#c62828',
  warning: '#e65100',
  badge: '#d32f2f',
};

const DARK: Colors = {
  background: '#111418',
  surface: '#1d2228',
  text: '#e5e7eb',
  textMuted: '#9ca3af',
  border: '#374151',
  primary: '#64b5f6',
  onPrimary: '#0b1220',
  danger: '#ef5350',
  warning: '#ffb74d',
  badge: '#ef5350',
};

export function useColors(): Colors {
  return useColorScheme() === 'dark' ? DARK : LIGHT;
}

/** The colour of a notification's severity (`INFO`, `IMPORTANT`, `URGENT`), null for none. */
export function severityColor(colors: Colors, severity?: string | null): string | null {
  switch (severity) {
    case 'URGENT':
      return colors.danger;
    case 'IMPORTANT':
      return colors.warning;
    case 'INFO':
      return colors.primary;
    default:
      return null;
  }
}
