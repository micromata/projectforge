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

import { StyleSheet, Text, View } from 'react-native';

import { t } from '../../core/i18n/translations';
import { alertMessage, useUserStatus } from '../../core/status/userStatus';
import { useColors } from '../../core/ui/colors';

/** The message of the administrators (e.g. maintenance) above a tab that doesn't show it itself (not the pages). */
export function AlertBanner() {
  const colors = useColors();
  const message = alertMessage(useUserStatus().status);
  if (!message) return null;
  return (
    <View style={[styles.banner, { backgroundColor: colors.warning }]} accessibilityRole="alert">
      <Text style={styles.text}>
        <Text style={styles.title}>{t('app.systemMessage.title')}: </Text>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { paddingHorizontal: 12, paddingVertical: 8 },
  text: { color: '#000', fontSize: 14 },
  title: { fontWeight: '600' },
});
