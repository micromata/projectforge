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

import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { alertMessage, useUserStatus } from '../../core/status/userStatus';
import { useColors } from '../../core/ui/colors';

/**
 * The message of the administrators (e.g. maintenance) above a tab that doesn't show it itself (not the pages). It
 * looks like the banner of next (`system-alert-banner.tsx`): its colours, the bell, the message only.
 */
export function AlertBanner() {
  const colors = useColors();
  const message = alertMessage(useUserStatus().status);
  if (!message) return null;
  return (
    <View style={[styles.banner, { backgroundColor: colors.alert }]} accessibilityRole="alert">
      <Ionicons name="notifications-outline" size={18} color={colors.onAlert} style={styles.icon} />
      <Text style={[styles.text, { color: colors.onAlert }]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { flexDirection: 'row', alignItems: 'flex-start', gap: 8, paddingHorizontal: 16, paddingVertical: 8 },
  icon: { marginTop: 1 },
  text: { flex: 1, fontSize: 14, fontWeight: '500' },
});
