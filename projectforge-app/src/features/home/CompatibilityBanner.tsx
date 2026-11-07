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
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { CompatibilityIssue } from '../../core/compat/compatibility';
import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { compatibilityText } from './TabBar';

/** The warning that the app and the server may not work together, above the tabs until it is confirmed. */
export function CompatibilityBanner({ issue, onConfirm }: { issue: CompatibilityIssue; onConfirm: () => void }) {
  const colors = useColors();
  return (
    <View style={[styles.banner, { backgroundColor: colors.surface, borderColor: colors.border }]} accessibilityRole="alert">
      <Ionicons name="warning-outline" size={22} color={colors.danger} />
      <Text style={[styles.text, { color: colors.text }]}>{compatibilityText(issue)}</Text>
      <Pressable hitSlop={8} onPress={onConfirm}>
        <Text style={[styles.ok, { color: colors.primary }]}>{t('ok')}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  text: { flex: 1, fontSize: 14 },
  ok: { fontSize: 16, fontWeight: '600' },
});
