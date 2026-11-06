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

import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

export interface FindResult {
  count: number;
  index: number;
  /** The page supports the CSS Custom Highlight API (Safari 17.2+, Chrome 105+). */
  supported: boolean;
}

/** The bar of the search in the page (see findInPage.ts). */
export function FindBar({
  result,
  onSearch,
  onClose,
}: {
  result: FindResult | null;
  onSearch: (query: string, direction: 'first' | 'next' | 'previous') => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState('');
  const status =
    result == null || !query
      ? ''
      : !result.supported
        ? 'Not supported'
        : result.count === 0
          ? '0/0'
          : `${result.index + 1}/${result.count}`;
  return (
    <View style={styles.bar}>
      <TextInput
        style={styles.input}
        autoFocus
        autoCapitalize="none"
        autoCorrect={false}
        placeholder="Find in page"
        value={query}
        returnKeyType="search"
        onChangeText={(text) => {
          setQuery(text);
          onSearch(text, 'first');
        }}
        onSubmitEditing={() => onSearch(query, 'next')}
      />
      <Text style={styles.status}>{status}</Text>
      <Pressable style={styles.icon} onPress={() => onSearch(query, 'previous')}>
        <Text style={styles.iconText}>▲</Text>
      </Pressable>
      <Pressable style={styles.icon} onPress={() => onSearch(query, 'next')}>
        <Text style={styles.iconText}>▼</Text>
      </Pressable>
      <Pressable style={styles.icon} onPress={onClose}>
        <Text style={styles.iconText}>✕</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f2f2f2',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#999',
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  input: { flex: 1, fontSize: 16, paddingVertical: 6, paddingHorizontal: 8, backgroundColor: '#fff', borderRadius: 6 },
  status: { minWidth: 56, textAlign: 'center', color: '#555' },
  icon: { paddingHorizontal: 10, paddingVertical: 6 },
  iconText: { fontSize: 16 },
});
