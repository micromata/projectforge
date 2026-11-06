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

import { useSyncExternalStore } from 'react';
import * as Clipboard from 'expo-clipboard';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { t } from '../i18n/translations';
import { useColors } from '../ui/colors';
import { appLog } from './appLog';

/** The client log (secrets masked by LogSanitizer), newest first; copied for a support request. */
export function LogScreen() {
  const colors = useColors();
  const entries = useSyncExternalStore(
    (listener) => appLog.subscribe(listener),
    () => appLog.all(),
  );
  const text = () =>
    entries.map((it) => `${it.time.toISOString()} ${it.level} ${it.area}: ${it.message}`).join('\n');
  return (
    <View style={[styles.fill, { backgroundColor: colors.background }]}>
      <Text style={[styles.info, { color: colors.textMuted }]}>{t('app.log.info')}</Text>
      <View style={styles.bar}>
        <Pressable
          onPress={async () => {
            await Clipboard.setStringAsync(text());
            Alert.alert(t('app.log.copied'));
          }}
          disabled={!entries.length}
        >
          <Text style={{ color: colors.primary, fontSize: 16 }}>{t('copy')}</Text>
        </Pressable>
        <Pressable onPress={() => appLog.clear()} disabled={!entries.length}>
          <Text style={{ color: colors.primary, fontSize: 16 }}>{t('app.log.clear')}</Text>
        </Pressable>
      </View>
      <FlatList
        data={[...entries].reverse()}
        keyExtractor={(_, index) => String(index)}
        ListEmptyComponent={<Text style={[styles.info, { color: colors.textMuted }]}>{t('app.log.empty')}</Text>}
        renderItem={({ item }) => (
          <Text
            selectable
            style={[
              styles.line,
              { color: item.level === 'warn' ? colors.warning : item.level === 'error' ? colors.danger : colors.text },
            ]}
          >
            {item.time.toTimeString().substring(0, 8)} {item.area}: {item.message}
          </Text>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  info: { fontSize: 13, paddingHorizontal: 12, paddingTop: 12 },
  bar: { flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: 12, paddingVertical: 10 },
  line: { fontFamily: 'Menlo', fontSize: 11, paddingHorizontal: 8, paddingVertical: 2 },
});
