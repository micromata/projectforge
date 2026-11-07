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
import type { Menus } from '@projectforge/shared/rs/menu';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  BackHandler,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import type { ApiClient } from '../../core/api/apiClient';
import { authController } from '../../core/auth/authController';
import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { KeyboardAvoidingFill } from '../../core/ui/KeyboardAvoidingFill';
import { MenuRepository } from './menuRepository';
import { accountTitle, MenuEntry, menuPath, MenuRow, menuRows } from './menuModel';

/** The menus loaded last, shown at once the next time (and refreshed): per login, as they depend on the user. */
let loaded: { api: ApiClient; menus: Menus } | null = null;

/**
 * The menu of the pages tab, over its page: the user menu (at the right, as on the web pages), the entries opened
 * last, the favorites and the main menu, and a search in all of them. On a phone this is easier to reach than the small
 * navigation of the web pages. [onOpen] gets the path (or url) of the chosen entry; [active]: the tab is shown (the
 * back button of Android closes the menu then).
 */
export function WebMenu({
  active,
  onOpen,
  onClose,
}: {
  active: boolean;
  onOpen: (path: string) => void;
  onClose: () => void;
}) {
  const colors = useColors();
  const api = authController.api;
  const repository = useMemo(() => (api ? new MenuRepository(api) : null), [api]);
  const [menus, setMenus] = useState<Menus | null>(loaded != null && loaded.api === api ? loaded.menus : null);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [term, setTerm] = useState('');
  const [openGroups, setOpenGroups] = useState<ReadonlySet<string>>(new Set());
  const [accountOpen, setAccountOpen] = useState(false);

  const load = useCallback((): Promise<void> => {
    if (api == null || repository == null) return Promise.resolve();
    return repository.load().then(
      (result) => {
        loaded = { api, menus: result };
        setMenus(result);
        setError(null);
      },
      (e: unknown) => setError(errorText(e)),
    );
  }, [api, repository]);

  // Each time it is shown: the favorites may have been changed in the web pages, the entries opened last have.
  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!active) return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      onClose();
      return true;
    });
    return () => subscription.remove();
  }, [active, onClose]);

  const rows = useMemo(
    () =>
      menus == null
        ? []
        : menuRows(
            menus,
            {
              mainMenu: t('menu.main.title'),
              favorites: t('favorites'),
              myAccount: t('menu.myAccount'),
              recent: t('menu.quickAccess.recent'),
            },
            { term, openGroups, accountOpen },
          ),
    [menus, term, openGroups, accountOpen],
  );

  const open = (entry: MenuEntry) => {
    if (entry.menuKey) repository?.reportUsage(entry.menuKey);
    onOpen(menuPath(entry.url));
  };

  const toggleGroup = (key: string) =>
    setOpenGroups((groups) => {
      const result = new Set(groups);
      if (!result.delete(key)) result.add(key);
      return result;
    });

  const pullToRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  const name = menus ? accountTitle(menus) : undefined;

  return (
    <View style={[StyleSheet.absoluteFill, { backgroundColor: colors.background }]}>
      <KeyboardAvoidingFill>
        <View style={[styles.top, { borderColor: colors.border }]}>
          {name != null && (
            <Pressable
              style={({ pressed }) => [styles.account, pressed && { backgroundColor: colors.surface }]}
              accessibilityRole="button"
              accessibilityState={{ expanded: accountOpen }}
              onPress={() => setAccountOpen((it) => !it)}
            >
              <Ionicons name="person-circle-outline" size={24} color={colors.primary} />
              <Text style={[styles.accountName, { color: colors.text }]} numberOfLines={1}>
                {name}
              </Text>
              <Ionicons name={accountOpen ? 'chevron-up' : 'chevron-down'} size={16} color={colors.textMuted} />
            </Pressable>
          )}
        </View>
        <View style={styles.fill}>
          {menus == null ? (
            error != null ? (
              <ErrorView message={error} onRetry={() => void load()} />
            ) : (
              <ActivityIndicator style={styles.loading} />
            )
          ) : (
            <FlatList
              data={rows}
              keyExtractor={(it) => it.key}
              keyboardShouldPersistTaps="handled"
              keyboardDismissMode="on-drag"
              refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void pullToRefresh()} />}
              renderItem={({ item }) => <Row row={item} onEntry={open} onGroup={toggleGroup} onSearchData={onOpen} />}
            />
          )}
        </View>
        <View style={[styles.searchRow, { borderColor: colors.border }]}>
          <View style={[styles.search, { backgroundColor: colors.surface }]}>
            <Ionicons name="search" size={18} color={colors.textMuted} />
            <TextInput
              style={[styles.searchInput, { color: colors.text }]}
              value={term}
              onChangeText={setTerm}
              placeholder={t('menu.quickAccess.placeholder')}
              placeholderTextColor={colors.textMuted}
              autoCorrect={false}
              autoCapitalize="none"
              returnKeyType="search"
              clearButtonMode="while-editing"
            />
          </View>
        </View>
      </KeyboardAvoidingFill>
    </View>
  );
}

function Row({
  row,
  onEntry,
  onGroup,
  onSearchData,
}: {
  row: MenuRow;
  onEntry: (entry: MenuEntry) => void;
  onGroup: (key: string) => void;
  onSearchData: (path: string) => void;
}) {
  const colors = useColors();
  switch (row.kind) {
    case 'heading':
      return (
        <Text
          style={[row.sub ? styles.subHeading : styles.heading, { color: colors.textMuted }]}
          accessibilityRole="header"
          numberOfLines={1}
        >
          {row.title}
        </Text>
      );
    case 'entry':
      return (
        <Pressable
          style={({ pressed }) => [
            styles.row,
            row.indent && styles.indent,
            pressed && { backgroundColor: colors.surface },
          ]}
          accessibilityRole="link"
          onPress={() => onEntry(row.entry)}
        >
          <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
            {row.entry.title}
          </Text>
          <Badge counter={row.entry.badgeCounter} />
        </Pressable>
      );
    case 'group':
      return (
        <Pressable
          style={({ pressed }) => [styles.row, pressed && { backgroundColor: colors.surface }]}
          accessibilityRole="button"
          accessibilityState={{ expanded: row.open }}
          onPress={() => onGroup(row.key)}
        >
          <Text style={[styles.title, styles.groupTitle, { color: colors.text }]} numberOfLines={1}>
            {row.title}
          </Text>
          <Badge counter={row.badgeCounter} />
          <Ionicons name={row.open ? 'chevron-up' : 'chevron-down'} size={16} color={colors.textMuted} />
        </Pressable>
      );
    case 'searchData':
      return (
        <Pressable
          style={({ pressed }) => [styles.row, pressed && { backgroundColor: colors.surface }]}
          accessibilityRole="link"
          onPress={() => onSearchData(`/next/search?q=${encodeURIComponent(row.term)}`)}
        >
          <Ionicons name="search" size={18} color={colors.primary} />
          <Text style={[styles.title, { color: colors.primary }]} numberOfLines={2}>
            {t('menu.quickAccess.searchAllData', row.term)}
          </Text>
        </Pressable>
      );
    case 'noMatch':
      return <Text style={[styles.noMatch, { color: colors.textMuted }]}>{t('app.web.menu.noMatch')}</Text>;
  }
}

function Badge({ counter }: { counter?: number }) {
  const colors = useColors();
  if (counter == null || counter <= 0) return null;
  return (
    <View style={[styles.badge, { backgroundColor: colors.badge }]}>
      <Text style={styles.badgeText}>{counter > 99 ? '99+' : counter}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  loading: { marginTop: 32 },
  top: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  account: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    maxWidth: '80%',
    height: 40,
    paddingHorizontal: 8,
    borderRadius: 20,
  },
  accountName: { flexShrink: 1, fontSize: 15 },
  heading: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 4,
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  subHeading: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 2, fontSize: 13, fontWeight: '500' },
  row: { minHeight: 44, flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 16 },
  indent: { paddingLeft: 32 },
  title: { flex: 1, fontSize: 16 },
  groupTitle: { fontWeight: '500' },
  noMatch: { paddingHorizontal: 16, paddingVertical: 12 },
  badge: {
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    paddingHorizontal: 5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#fff', fontSize: 12, fontWeight: '600' },
  searchRow: { paddingHorizontal: 12, paddingVertical: 6, borderTopWidth: StyleSheet.hairlineWidth },
  search: { flexDirection: 'row', alignItems: 'center', gap: 8, borderRadius: 22, height: 40, paddingHorizontal: 12 },
  searchInput: { flex: 1, fontSize: 16, paddingVertical: 0 },
});
