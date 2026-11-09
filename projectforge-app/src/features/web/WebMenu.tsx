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

import { Ionicons } from "@expo/vector-icons";
import type { Menus } from "@projectforge/shared/rs/menu";
import { useCallback, useEffect, useMemo, useState } from "react";
import { GestureDetector } from "react-native-gesture-handler";
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";

import type { ApiClient } from "../../core/api/apiClient";
import { authController } from "../../core/auth/authController";
import { errorText, t } from "../../core/i18n/translations";
import {
  ThemeMode,
  userStatus,
  useUserStatus,
} from "../../core/status/userStatus";
import { useColors } from "../../core/ui/colors";
import { ErrorView } from "../../core/ui/ErrorView";
import { KeyboardAvoidingFill } from "../../core/ui/KeyboardAvoidingFill";
import { SheetPull, TabSheet } from "../../core/ui/TabSheet";
import { GroupStates, menuGroups } from "./menuGroups";
import { MenuRepository } from "./menuRepository";
import {
  defaultTab,
  menuColumns,
  MenuEntry,
  menuPath,
  MenuRow,
  menuRows,
  MenuTab,
  tabBadge,
} from "./menuModel";

/** The menus loaded last, shown at once the next time (and refreshed): per login, as they depend on the user. */
let loaded: { api: ApiClient; menus: Menus } | null = null;

/** The tab chosen last, shown again the next time the menu opens. */
let chosenTab: MenuTab | null = null;
const rememberTab = (tab: MenuTab) => {
  chosenTab = tab;
};

/** The categories the user toggled (see MenuGroups), read once per login. */
let restoredGroups: { api: ApiClient; states: GroupStates } | null = null;

const MIN_COLUMN_WIDTH = 300;
const MAX_COLUMNS = 4;

const TABS: { tab: MenuTab; label: () => string }[] = [
  { tab: "favorites", label: () => t("favorites") },
  { tab: "main", label: () => t("menu.main.title") },
  { tab: "recent", label: () => t("app.web.menu.recent") },
  { tab: "account", label: () => t("menu.myAccount") },
];

/**
 * The menu of the pages tab, a sheet moved up over its page: tabs for the favorites, the main menu, the entries opened
 * last and the user menu (at the right on the web pages), and a search in all of them. On a phone this is easier to
 * reach than the small navigation of the web pages; on a wide screen (landscape, tablet) the rows are in columns and
 * the categories of the main menu open (unless the user closed them). The page stays visible above it, dimmed: a tap
 * there, pulling the sheet down by its top or the globe again closes it ([onClose]) and shows the page as it was.
 * [open]: shown (moved in and out); [onOpen] gets the path (or url) of the chosen entry; [active]: the tab is shown (the
 * back button of Android closes the menu then).
 */
export function WebMenu({
  open,
  active,
  onOpen,
  onClose,
}: {
  open: boolean;
  active: boolean;
  onOpen: (path: string) => void;
  onClose: () => void;
}) {
  return (
    <TabSheet
      open={open}
      active={active}
      closeLabel={t("app.web.menu.close")}
      onClose={onClose}
    >
      {(pull) => <MenuContent pull={pull} onOpen={onOpen} />}
    </TabSheet>
  );
}

function MenuContent({
  pull,
  onOpen,
}: {
  pull: SheetPull;
  onOpen: (path: string) => void;
}) {
  const colors = useColors();
  const api = authController.api;
  const repository = useMemo(
    () => (api ? new MenuRepository(api) : null),
    [api],
  );
  const [menus, setMenus] = useState<Menus | null>(
    loaded != null && loaded.api === api ? loaded.menus : null,
  );
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [term, setTerm] = useState("");
  const [groupStates, setGroupStates] = useState<GroupStates>(() => {
    if (restoredGroups == null || restoredGroups.api !== api)
      restoredGroups = api ? { api, states: menuGroups.restore() } : null;
    return restoredGroups?.states ?? {};
  });
  const [tabChosen, setTabChosen] = useState<MenuTab | null>(chosenTab);
  // Wide (landscape, tablet): the rows in columns.
  const [width, setWidth] = useState(0);
  const columns = Math.min(
    MAX_COLUMNS,
    Math.max(1, Math.floor(width / MIN_COLUMN_WIDTH)),
  );
  // Before the first layout (width 0) the default of a phone: on a wide screen the categories open with the columns.
  const groupsOpenByDefault = columns > 1;
  const tab = tabChosen ?? (menus ? defaultTab(menus) : "favorites");
  const choose = (next: MenuTab) => {
    rememberTab(next);
    setTabChosen(next);
  };

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

  const rows = useMemo(
    () =>
      menus == null
        ? []
        : menuRows(
            menus,
            {
              mainMenu: t("menu.main.title"),
              favorites: t("favorites"),
              myAccount: t("menu.myAccount"),
              recent: t("menu.quickAccess.recent"),
            },
            { term, groupStates, groupsOpenByDefault, tab },
          ),
    [menus, term, groupStates, groupsOpenByDefault, tab],
  );

  const open = (entry: MenuEntry) => {
    if (entry.menuKey) repository?.reportUsage(entry.menuKey);
    onOpen(menuPath(entry.url));
  };

  const toggleGroup = (key: string) => {
    const states = {
      ...groupStates,
      [key]: !(groupStates[key] ?? groupsOpenByDefault),
    };
    if (api) restoredGroups = { api, states };
    menuGroups.remember(states);
    setGroupStates(states);
  };

  const pullToRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };
  const refreshControl = (
    <RefreshControl
      refreshing={refreshing}
      onRefresh={() => void pullToRefresh()}
    />
  );

  return (
    <View
      style={[
        styles.fill,
        styles.content,
        { backgroundColor: colors.background },
      ]}
    >
      <KeyboardAvoidingFill>
        <GestureDetector gesture={pull}>
          <View>
            <View style={styles.handleArea}>
              <View
                style={[styles.handle, { backgroundColor: colors.textMuted }]}
              />
            </View>
            <View
              style={[styles.tabs, { borderColor: colors.border }]}
              accessibilityRole="tablist"
            >
              {TABS.map((it) => (
                <TabButton
                  key={it.tab}
                  label={it.label()}
                  // While searching the hits come from all tabs: none is chosen.
                  selected={!term.trim() && it.tab === tab}
                  badgeCounter={menus ? tabBadge(menus, it.tab) : undefined}
                  onPress={() => {
                    setTerm("");
                    choose(it.tab);
                  }}
                />
              ))}
            </View>
          </View>
        </GestureDetector>
        <View
          style={styles.fill}
          onLayout={(event) => setWidth(event.nativeEvent.layout.width)}
        >
          {menus == null ? (
            error != null ? (
              <ErrorView message={error} onRetry={() => void load()} />
            ) : (
              <ActivityIndicator style={styles.loading} />
            )
          ) : columns > 1 ? (
            <ScrollView
              keyboardShouldPersistTaps="handled"
              keyboardDismissMode="on-drag"
              refreshControl={refreshControl}
            >
              <View style={styles.columns}>
                {menuColumns(rows, columns).map((column) => (
                  <View key={column[0].key} style={styles.fill}>
                    {column.map((row) => (
                      <Row
                        key={row.key}
                        row={row}
                        onEntry={open}
                        onGroup={toggleGroup}
                        onSearchData={onOpen}
                      />
                    ))}
                  </View>
                ))}
              </View>
            </ScrollView>
          ) : (
            <FlatList
              data={rows}
              keyExtractor={(it) => it.key}
              keyboardShouldPersistTaps="handled"
              keyboardDismissMode="on-drag"
              refreshControl={refreshControl}
              renderItem={({ item }) => (
                <Row
                  row={item}
                  onEntry={open}
                  onGroup={toggleGroup}
                  onSearchData={onOpen}
                />
              )}
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
              placeholder={t("menu.quickAccess.placeholder")}
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
    case "heading":
      return (
        <Text
          style={[
            row.sub ? styles.subHeading : styles.heading,
            { color: colors.textMuted },
          ]}
          accessibilityRole="header"
          numberOfLines={1}
        >
          {row.title}
        </Text>
      );
    case "entry":
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
          <Text
            style={[styles.title, { color: colors.text }]}
            numberOfLines={1}
          >
            {row.entry.title}
          </Text>
          <Badge counter={row.entry.badgeCounter} />
        </Pressable>
      );
    case "group":
      return (
        <Pressable
          style={({ pressed }) => [
            styles.row,
            pressed && { backgroundColor: colors.surface },
          ]}
          accessibilityRole="button"
          accessibilityState={{ expanded: row.open }}
          onPress={() => onGroup(row.key)}
        >
          <Text
            style={[styles.title, styles.groupTitle, { color: colors.text }]}
            numberOfLines={1}
          >
            {row.title}
          </Text>
          <Badge counter={row.badgeCounter} />
          <Ionicons
            name={row.open ? "chevron-up" : "chevron-down"}
            size={16}
            color={colors.textMuted}
          />
        </Pressable>
      );
    case "searchData":
      return (
        <Pressable
          style={({ pressed }) => [
            styles.row,
            pressed && { backgroundColor: colors.surface },
          ]}
          accessibilityRole="link"
          onPress={() =>
            onSearchData(`/next/search?q=${encodeURIComponent(row.term)}`)
          }
        >
          <Ionicons name="search" size={18} color={colors.primary} />
          <Text
            style={[styles.title, { color: colors.primary }]}
            numberOfLines={2}
          >
            {t("menu.quickAccess.searchAllData", row.term)}
          </Text>
        </Pressable>
      );
    case "noMatch":
      return (
        <Text style={[styles.noMatch, { color: colors.textMuted }]}>
          {t("app.web.menu.noMatch")}
        </Text>
      );
    case "empty":
      return (
        <Text style={[styles.noMatch, { color: colors.textMuted }]}>
          {t("app.web.menu.empty")}
        </Text>
      );
    case "theme":
      return <ThemeRow />;
  }
}

const THEMES: { theme: ThemeMode; label: () => string }[] = [
  { theme: "light", label: () => t("app.theme.light") },
  { theme: "dark", label: () => t("app.theme.dark") },
  { theme: "system", label: () => t("app.theme.system") },
];

/** Light, dark or as the system: stored for the user as the web pages do it (see UserStatusStore.chooseTheme). */
function ThemeRow() {
  const colors = useColors();
  const { theme } = useUserStatus();
  const choose = (next: ThemeMode) =>
    userStatus
      .chooseTheme(next)
      .catch((e: unknown) => Alert.alert(errorText(e)));
  return (
    <View style={[styles.row, styles.themeRow, { borderColor: colors.border }]}>
      <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
        {t("app.theme")}
      </Text>
      <View
        style={[styles.segments, { borderColor: colors.border }]}
        accessibilityRole="radiogroup"
      >
        {THEMES.map((it) => {
          const selected = it.theme === theme;
          return (
            <Pressable
              key={it.theme}
              style={[
                styles.segment,
                selected && { backgroundColor: colors.primary },
              ]}
              accessibilityRole="radio"
              accessibilityState={{ selected }}
              onPress={() => void choose(it.theme)}
            >
              <Text
                style={[
                  styles.segmentText,
                  { color: selected ? "#fff" : colors.text },
                ]}
              >
                {it.label()}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function TabButton({
  label,
  selected,
  badgeCounter,
  onPress,
}: {
  label: string;
  selected: boolean;
  badgeCounter?: number;
  onPress: () => void;
}) {
  const colors = useColors();
  const color = selected ? colors.primary : colors.textMuted;
  return (
    <Pressable
      style={({ pressed }) => [
        styles.tab,
        { borderColor: selected ? colors.primary : "transparent" },
        pressed && { backgroundColor: colors.surface },
      ]}
      accessibilityRole="tab"
      accessibilityState={{ selected }}
      accessibilityLabel={label}
      onPress={onPress}
    >
      <Text style={[styles.tabLabel, { color }]} numberOfLines={1}>
        {label}
      </Text>
      <Badge counter={badgeCounter} small />
    </Pressable>
  );
}

function Badge({
  counter,
  small = false,
}: {
  counter?: number;
  small?: boolean;
}) {
  const colors = useColors();
  if (counter == null || counter <= 0) return null;
  return (
    <View
      style={[
        styles.badge,
        small && styles.smallBadge,
        { backgroundColor: colors.badge },
      ]}
    >
      <Text style={styles.badgeText}>{counter > 99 ? "99+" : counter}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  loading: { marginTop: 32 },
  columns: { flexDirection: "row" },
  content: {
    borderTopLeftRadius: 14,
    borderTopRightRadius: 14,
    overflow: "hidden",
  },
  handleArea: { alignItems: "center", paddingTop: 6, paddingBottom: 2 },
  handle: { width: 36, height: 5, borderRadius: 3, opacity: 0.5 },
  tabs: { flexDirection: "row", borderBottomWidth: StyleSheet.hairlineWidth },
  tab: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 4,
    height: 36,
    paddingHorizontal: 2,
    borderBottomWidth: 2,
  },
  tabLabel: { flexShrink: 1, fontSize: 13, fontWeight: "500" },
  heading: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 2,
    fontSize: 13,
    fontWeight: "600",
    textTransform: "uppercase",
  },
  subHeading: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 0,
    fontSize: 12,
    fontWeight: "500",
  },
  row: {
    minHeight: 30,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    paddingHorizontal: 16,
  },
  indent: { paddingLeft: 26 },
  title: { flex: 1, fontSize: 15 },
  groupTitle: { fontWeight: "500" },
  noMatch: { paddingHorizontal: 16, paddingVertical: 12 },
  badge: {
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    paddingHorizontal: 5,
    alignItems: "center",
    justifyContent: "center",
  },
  themeRow: {
    marginTop: 8,
    paddingTop: 6,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  segments: {
    flexDirection: "row",
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 8,
    overflow: "hidden",
  },
  segment: { paddingHorizontal: 10, paddingVertical: 5 },
  segmentText: { fontSize: 13 },
  smallBadge: {
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 4,
  },
  badgeText: { color: "#fff", fontSize: 12, fontWeight: "600" },
  searchRow: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  search: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    borderRadius: 22,
    height: 40,
    paddingHorizontal: 12,
  },
  searchInput: { flex: 1, fontSize: 16, paddingVertical: 0 },
});
