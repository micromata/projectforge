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
import { useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { KeyboardAvoidingFill } from '../../core/ui/KeyboardAvoidingFill';
import { HeaderAction, ScreenHeader } from '../../core/ui/ScreenHeader';
import { AddressBook, AppAddress, itemOffsets, listItems, ListItem, matches, sectionIndex, sectionLetter, sortKey, subtitle } from './address';
import { AddressAvatar } from './AddressAvatar';
import { addresses, useAddresses } from './addressesStore';

const ROW_HEIGHT = 64;
const HEADER_HEIGHT = 32;
/** Fewer addresses need no index at the side. */
const INDEX_MINIMUM = 20;
const LETTERS = [...'ABCDEFGHIJKLMNOPQRSTUVWXYZ', '#'];

/** Loads the changes from the server; a failure is reported in an alert, the shown addresses stay. */
async function refreshAddresses() {
  const error = await addresses.refresh();
  if (error != null) Alert.alert(errorText(error));
}

/**
 * The address book like the one of Apple: sections by the first letter with an index at the side, a search field,
 * and the address with its numbers and mail addresses on tap ([onShow]). Editing is done in the web pages
 * ([onCreate]). Port of the Flutter app's AddressesScreen.
 */
export function AddressesScreen({ onShow, onCreate }: { onShow: (id: number) => void; onCreate: () => void }) {
  const colors = useColors();
  const state = useAddresses();
  let body;
  if (state.status === 'loading') body = <ActivityIndicator style={styles.fill} />;
  else if (state.status === 'error') {
    body = <ErrorView message={errorText(state.error)} onRetry={() => void addresses.refresh()} />;
  } else body = <AddressList book={state.book} onShow={onShow} onCreate={onCreate} />;
  return (
    <View style={[styles.fill, { backgroundColor: colors.background }]}>
      <ScreenHeader title={t('menu.addressList')}>
        <HeaderAction icon="refresh" label={t('refresh')} onPress={() => void refreshAddresses()} />
      </ScreenHeader>
      {body}
    </View>
  );
}

function AddressList({
  book,
  onShow,
  onCreate,
}: {
  book: AddressBook;
  onShow: (id: number) => void;
  onCreate: () => void;
}) {
  const colors = useColors();
  const list = useRef<FlatList<ListItem>>(null);
  const [query, setQuery] = useState('');
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const favorites = useMemo(() => new Set(book.favoriteIds), [book.favoriteIds]);
  const shown = useMemo(
    () => book.addresses.filter((it) => (!favoritesOnly || favorites.has(it.id)) && matches(it, query)),
    [book.addresses, favorites, favoritesOnly, query],
  );
  const items = useMemo(() => listItems(shown), [shown]);
  // The offset of every item: the rows and headers have fixed heights, so the index can jump exactly.
  const offsets = useMemo(() => itemOffsets(items, HEADER_HEIGHT, ROW_HEIGHT), [items]);
  const searching = query.trim().length > 0;
  const showIndex = !searching && shown.length >= INDEX_MINIMUM;

  const pullToRefresh = async () => {
    setRefreshing(true);
    await refreshAddresses();
    setRefreshing(false);
  };

  return (
    <KeyboardAvoidingFill>
      {book.fromCache && <Text style={[styles.offline, { color: colors.danger }]}>{t('app.address.offline')}</Text>}
      <View style={[styles.segments, { borderColor: colors.border }]}>
        {[true, false].map((favorite) => (
          <Pressable
            key={String(favorite)}
            style={[styles.segment, favoritesOnly === favorite && { backgroundColor: colors.surface }]}
            accessibilityRole="tab"
            accessibilityState={{ selected: favoritesOnly === favorite }}
            onPress={() => setFavoritesOnly(favorite)}
          >
            {favorite && <Ionicons name="star-outline" size={16} color={colors.text} />}
            <Text style={{ color: colors.text }}>{favorite ? t('favorites') : t('app.address.all')}</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.fill}>
        <FlatList
          ref={list}
          data={items}
          keyExtractor={(it) => (it.type === 'header' ? `#${it.letter}` : `${it.address.id}`)}
          getItemLayout={(_, index) => ({
            length: items[index]?.type === 'header' ? HEADER_HEIGHT : ROW_HEIGHT,
            offset: offsets[index] ?? 0,
            index,
          })}
          contentContainerStyle={{ paddingRight: showIndex ? 20 : 0 }}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void pullToRefresh()} />}
          ListEmptyComponent={
            <Text style={[styles.empty, { color: colors.text }]}>
              {searching ? t('app.address.noMatch') : favoritesOnly ? t('app.address.noFavorites') : t('app.address.none')}
            </Text>
          }
          renderItem={({ item }) =>
            item.type === 'header' ? (
              <View style={styles.header}>
                <Text style={{ color: colors.textMuted, fontWeight: '500' }}>{item.letter}</Text>
              </View>
            ) : (
              <AddressRow address={item.address} favorite={favorites.has(item.address.id)} onPress={() => onShow(item.address.id)} />
            )
          }
        />
        {showIndex && (
          <IndexBar
            existing={new Set(shown.map(sectionLetter))}
            onLetter={(letter) =>
              list.current?.scrollToOffset({ offset: offsets[sectionIndex(items, letter)] ?? 0, animated: false })
            }
          />
        )}
      </View>
      <View style={[styles.searchRow, { borderColor: colors.border }]}>
        <View style={[styles.search, { backgroundColor: colors.surface }]}>
          <Ionicons name="search" size={18} color={colors.textMuted} />
          <TextInput
            style={[styles.searchInput, { color: colors.text }]}
            value={query}
            onChangeText={setQuery}
            placeholder={t('search')}
            placeholderTextColor={colors.textMuted}
            autoCorrect={false}
            returnKeyType="search"
            clearButtonMode="while-editing"
          />
        </View>
        <HeaderAction icon="add" label={t('app.address.new')} onPress={onCreate} />
      </View>
    </KeyboardAvoidingFill>
  );
}

function AddressRow({ address, favorite, onPress }: { address: AppAddress; favorite: boolean; onPress: () => void }) {
  const colors = useColors();
  const name = sortKey(address);
  const second = subtitle(address);
  return (
    <Pressable style={({ pressed }) => [styles.row, pressed && { backgroundColor: colors.surface }]} onPress={onPress}>
      <AddressAvatar address={address} size={44} />
      <View style={styles.fill}>
        <Text style={[styles.name, { color: colors.text }]} numberOfLines={1}>
          {name || '–'}
        </Text>
        {second && (
          <Text style={{ color: colors.textMuted }} numberOfLines={1}>
            {second}
          </Text>
        )}
      </View>
      {favorite && <Ionicons name="star" size={18} color="#f59e0b" />}
    </Pressable>
  );
}

/** A to Z and "#" at the side: touching or dragging jumps to the section. */
function IndexBar({ existing, onLetter }: { existing: Set<string>; onLetter: (letter: string) => void }) {
  const colors = useColors();
  const [height, setHeight] = useState(0);
  const touched = useRef<string | null>(null);
  const touch = (y: number) => {
    if (height <= 0) return;
    const index = Math.max(0, Math.min(LETTERS.length - 1, Math.floor((y / height) * LETTERS.length)));
    const letter = LETTERS[index];
    if (letter === touched.current) return;
    touched.current = letter;
    onLetter(letter);
  };
  return (
    <View
      style={styles.index}
      onLayout={(e) => setHeight(e.nativeEvent.layout.height)}
      onStartShouldSetResponder={() => true}
      onMoveShouldSetResponder={() => true}
      onResponderGrant={(e) => touch(e.nativeEvent.locationY)}
      onResponderMove={(e) => touch(e.nativeEvent.locationY)}
      onResponderRelease={() => (touched.current = null)}
      onResponderTerminate={() => (touched.current = null)}
    >
      {LETTERS.map((letter) => (
        <Text
          key={letter}
          style={[styles.indexLetter, { color: colors.primary, opacity: existing.has(letter) ? 1 : 0.35 }]}
        >
          {letter}
        </Text>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  offline: { padding: 8 },
  segments: {
    flexDirection: 'row',
    marginHorizontal: 16,
    marginVertical: 6,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 18,
    overflow: 'hidden',
  },
  segment: { flex: 1, flexDirection: 'row', gap: 6, alignItems: 'center', justifyContent: 'center', height: 34 },
  empty: { padding: 32, textAlign: 'center' },
  header: { height: HEADER_HEIGHT, paddingHorizontal: 16, paddingBottom: 4, justifyContent: 'flex-end' },
  row: { height: ROW_HEIGHT, flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16 },
  name: { fontSize: 16, fontWeight: '600' },
  index: { position: 'absolute', right: 0, top: 4, bottom: 4, width: 24, justifyContent: 'space-around', alignItems: 'center' },
  indexLetter: { fontSize: 11, fontWeight: '700' },
  searchRow: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 12, paddingVertical: 6, borderTopWidth: StyleSheet.hairlineWidth },
  search: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8, borderRadius: 22, height: 40, paddingHorizontal: 12 },
  searchInput: { flex: 1, fontSize: 16, paddingVertical: 0 },
});
