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
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { authController } from '../../core/auth/authController';
import { t } from '../../core/i18n/translations';
import { userStatus } from '../../core/status/userStatus';
import { useColors } from '../../core/ui/colors';

/**
 * The SSO proxy refuses the polling (UserStatusStore.proxyExpired): badges and notifications stand still. The polling
 * never opens the browser by itself, so the renewal of the login is offered here, above the tabs, until it is done.
 */
export function ProxyExpiredBanner() {
  const colors = useColors();
  const [renewing, setRenewing] = useState(false);

  const renew = async () => {
    setRenewing(true);
    try {
      if (await authController.reauthenticate()) await userStatus.refresh();
    } finally {
      setRenewing(false);
    }
  };

  return (
    <View style={[styles.banner, { backgroundColor: colors.surface, borderColor: colors.border }]} accessibilityRole="alert">
      <Ionicons name="lock-closed-outline" size={22} color={colors.warning} />
      <Text style={[styles.text, { color: colors.text }]}>{t('app.proxy.expired')}</Text>
      <Pressable hitSlop={8} disabled={renewing} onPress={() => void renew()} accessibilityRole="button">
        <Text style={[styles.action, { color: colors.primary, opacity: renewing ? 0.5 : 1 }]}>{t('app.proxy.renew')}</Text>
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
  action: { fontSize: 16, fontWeight: '600' },
});
