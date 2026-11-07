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
import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { authController, useAuth } from '../../core/auth/authController';
import { Session } from '../../core/auth/session';
import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';

/** The stored logins at other servers to switch to without the browser, and the login at another server. */
export function ServerSwitchScreen({ onDone, onAdd }: { onDone: () => void; onAdd: () => void }) {
  const colors = useColors();
  const auth = useAuth();
  const [sessions, setSessions] = useState<Session[]>([]);

  useEffect(() => {
    void authController.sessions().then(setSessions);
  }, [auth.session]);

  return (
    <ScrollView style={{ backgroundColor: colors.background }} contentContainerStyle={styles.container}>
      {sessions.map((session) => {
        const current = session.serverUrl === auth.session?.serverUrl;
        return (
          <Pressable
            key={session.serverUrl}
            style={[styles.row, { borderColor: colors.border }]}
            onPress={async () => {
              if (!current) await authController.switchTo(session);
              onDone();
            }}
          >
            <Ionicons name={current ? 'checkmark-circle' : 'server-outline'} size={22} color={current ? colors.primary : colors.textMuted} />
            <View style={styles.fill}>
              <Text style={{ color: colors.text, fontSize: 16 }} numberOfLines={1}>
                {session.serverUrl}
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 13 }}>{session.fullname ?? session.username ?? ''}</Text>
            </View>
          </Pressable>
        );
      })}
      <Pressable style={[styles.row, { borderColor: colors.border }]} onPress={onAdd}>
        <Ionicons name="add" size={22} color={colors.primary} />
        <Text style={{ color: colors.primary, fontSize: 16 }}>{t('app.server.add')}</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  container: { padding: 16 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, borderBottomWidth: StyleSheet.hairlineWidth },
});
