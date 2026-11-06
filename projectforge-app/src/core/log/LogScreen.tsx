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
import { FlatList, Pressable, Share, StyleSheet, Text, View } from 'react-native';

import { appLog } from './appLog';

/** The client log (secrets masked by LogSanitizer), newest first. */
export function LogScreen({ onClose }: { onClose: () => void }) {
  const entries = useSyncExternalStore(
    (listener) => appLog.subscribe(listener),
    () => appLog.all(),
  );
  const text = () =>
    entries.map((it) => `${it.time.toISOString()} ${it.level} ${it.area}: ${it.message}`).join('\n');
  return (
    <View style={styles.fill}>
      <View style={styles.bar}>
        <Pressable onPress={onClose}>
          <Text style={styles.action}>Close</Text>
        </Pressable>
        <Pressable onPress={() => Share.share({ message: text() })}>
          <Text style={styles.action}>Share</Text>
        </Pressable>
      </View>
      <FlatList
        data={[...entries].reverse()}
        keyExtractor={(_, index) => String(index)}
        renderItem={({ item }) => (
          <Text style={[styles.line, item.level === 'warn' && styles.warn, item.level === 'error' && styles.error]}>
            {item.time.toTimeString().substring(0, 8)} {item.area}: {item.message}
          </Text>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  bar: { flexDirection: 'row', justifyContent: 'space-between', padding: 12 },
  action: { color: '#1976d2', fontSize: 16 },
  line: { fontFamily: 'Menlo', fontSize: 11, paddingHorizontal: 8, paddingVertical: 2 },
  warn: { color: '#e65100' },
  error: { color: '#c62828' },
});
