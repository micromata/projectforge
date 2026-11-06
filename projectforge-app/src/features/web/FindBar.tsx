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

import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';

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
  const colors = useColors();
  const status =
    result == null || !query
      ? ''
      : !result.supported
        ? '–'
        : result.count === 0 && !result.supported
          ? '–'
          : result.count === 0
          ? '0/0'
          : `${result.index + 1}/${result.count}`;
  return (
    <View style={[styles.bar, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      <TextInput
        style={[styles.input, { backgroundColor: colors.background, color: colors.text }]}
        autoFocus
        autoCapitalize="none"
        autoCorrect={false}
        placeholder={t('app.web.find')}
        placeholderTextColor={colors.textMuted}
        value={query}
        returnKeyType="search"
        onChangeText={(text) => {
          setQuery(text);
          onSearch(text, 'first');
        }}
        onSubmitEditing={() => onSearch(query, 'next')}
      />
      <Text style={[styles.status, { color: colors.textMuted }]}>{status}</Text>
      <Pressable style={styles.icon} accessibilityLabel={t('app.web.find.previous')} onPress={() => onSearch(query, 'previous')}>
        <Text style={[styles.iconText, { color: colors.text }]}>▲</Text>
      </Pressable>
      <Pressable style={styles.icon} accessibilityLabel={t('app.web.find.next')} onPress={() => onSearch(query, 'next')}>
        <Text style={[styles.iconText, { color: colors.text }]}>▼</Text>
      </Pressable>
      <Pressable style={styles.icon} accessibilityLabel={t('close')} onPress={onClose}>
        <Text style={[styles.iconText, { color: colors.text }]}>✕</Text>
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
    borderBottomWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  input: { flex: 1, fontSize: 16, paddingVertical: 6, paddingHorizontal: 8, borderRadius: 6 },
  status: { minWidth: 56, textAlign: 'center' },
  icon: { paddingHorizontal: 10, paddingVertical: 6 },
  iconText: { fontSize: 16 },
});
