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
import { ComponentProps, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { TaskLabel } from './TaskLabel';
import { TaskRef } from './timesheet';
import type { TaskChild } from './timesheetsRepository';
import { timesheets } from './timesheetsStore';

type Loaded<T> = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'data'; value: T };

/** A task of the path to a level of the tree. */
interface Crumb {
  id: number;
  title: string;
}

/** [load] whenever [key] changes, the answer of the last call only. */
function useLoaded<T>(key: string | null, load: () => Promise<T>): [Loaded<T> | null, () => void] {
  const [state, setState] = useState<{ key: string; loaded: Loaded<T> } | null>(null);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (key == null) return;
    let cancelled = false;
    load().then(
      (value) => !cancelled && setState({ key, loaded: { status: 'data', value } }),
      (error: unknown) => !cancelled && setState({ key, loaded: { status: 'error', error } }),
    );
    return () => {
      cancelled = true;
    };
    // The key stands for the request.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, retry]);
  const loaded = key == null ? null : state?.key === key ? state.loaded : { status: 'loading' as const };
  return [loaded, () => setRetry((it) => it + 1)];
}

/** The search term, 300 ms after the last key stroke. */
function useDebounced(value: string): string {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timeout = setTimeout(() => setDebounced(value.trim()), 300);
    return () => clearTimeout(timeout);
  }, [value]);
  return debounced;
}

/**
 * The search for the task to book on: the bookable tasks matching the typed words of the path, without input the ones
 * used last and the way into the task tree (one level at a time, with the path above it and a search below it).
 * Port of the Flutter app's TaskPicker.
 *
 * @param recentFromBookings Tasks of the latest bookings, offered after the ones picked last.
 * @param current The task chosen so far: marked in the lists, and the tree opens at its level.
 */
export function TaskPickerScreen({
  recentFromBookings,
  current,
  onPick,
}: {
  recentFromBookings: TaskRef[];
  current: TaskRef | null;
  onPick: (task: TaskRef | null) => void;
}) {
  const colors = useColors();
  const [input, setInput] = useState('');
  const term = useDebounced(input);
  /** Null: the search; otherwise the level of the tree below the last task of the path. */
  const [tree, setTree] = useState<Crumb[] | null>(null);
  const repo = timesheets.repository();
  const [recentTasks] = useLoaded('recent', () => repo?.recentTasks() ?? Promise.resolve([]));

  const openTree = async () => {
    let path: Crumb[] = [];
    if (current && repo) {
      try {
        const info = await repo.taskInfo(current.id);
        if (info.pathIds.length === info.task.parents.length) {
          path = info.pathIds.map((id, i) => ({ id, title: info.task.parents[i] }));
        }
      } catch {
        // Then from the top only.
      }
    }
    setInput('');
    setTree(path);
  };

  if (tree != null) {
    return (
      <TaskTree
        path={tree}
        current={current}
        onPath={(path) => {
          setInput('');
          setTree(path);
        }}
        onPick={onPick}
      />
    );
  }
  const picked = recentTasks?.status === 'data' ? recentTasks.value : [];
  const tasks = [...picked, ...recentFromBookings].filter((it, i, all) => all.findIndex((x) => x.id === it.id) === i);
  return (
    <SafeAreaView style={[styles.fill, { backgroundColor: colors.background }]}>
      <View style={[styles.bar, { borderColor: colors.border }]}>
        <Pressable accessibilityLabel={t('back')} hitSlop={8} onPress={() => onPick(null)}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <TextInput
          style={[styles.search, { color: colors.text }]}
          value={input}
          onChangeText={setInput}
          placeholder={t('app.timesheet.task.search')}
          placeholderTextColor={colors.textMuted}
          autoFocus
          autoCorrect={false}
          clearButtonMode="while-editing"
        />
      </View>
      {term ? (
        <SearchResults term={term} current={current} onPick={onPick} />
      ) : (
        <ScrollView keyboardShouldPersistTaps="handled">
          <Row icon="git-network-outline" label={t('app.timesheet.task.tree')} chevron onPress={() => void openTree()} />
          {tasks.length === 0 ? (
            <Text style={[styles.hint, { color: colors.textMuted }]}>{t('app.timesheet.task.hint')}</Text>
          ) : (
            <>
              <Text style={[styles.header, { color: colors.primary }]}>{t('timesheet.recent')}</Text>
              {tasks.map((it) => (
                <TaskRow key={it.id} task={it} selected={it.id === current?.id} onPress={() => onPick(it)} />
              ))}
            </>
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

/** The bookable tasks matching [term], below the task [parentId] only if given. */
function SearchResults({
  term,
  parentId,
  current,
  onPick,
}: {
  term: string;
  parentId?: number;
  current: TaskRef | null;
  onPick: (task: TaskRef) => void;
}) {
  const colors = useColors();
  const [results, retry] = useLoaded(`${parentId}|${term}`, () =>
    timesheets.repository()?.searchTasks(term, parentId) ?? Promise.resolve([]),
  );
  if (results == null || results.status === 'loading') return <ActivityIndicator style={styles.fill} />;
  if (results.status === 'error') return <ErrorView message={errorText(results.error)} onRetry={retry} />;
  if (results.value.length === 0) return <Text style={[styles.hint, { color: colors.text }]}>{t('app.timesheet.task.none')}</Text>;
  const words = term.toLowerCase().split(/\s+/);
  return (
    <ScrollView keyboardShouldPersistTaps="handled">
      {results.value.map((it) => (
        <TaskRow key={it.id} task={it} words={words} selected={it.id === current?.id} onPress={() => onPick(it)} />
      ))}
      <Text style={[styles.hint, { color: colors.textMuted }]}>{t('app.timesheet.task.onlyBookable')}</Text>
    </ScrollView>
  );
}

/** One level of the task tree: the children of the last task of [path] (the top level if empty). */
function TaskTree({
  path,
  current,
  onPath,
  onPick,
}: {
  path: Crumb[];
  current: TaskRef | null;
  onPath: (path: Crumb[]) => void;
  onPick: (task: TaskRef | null) => void;
}) {
  const colors = useColors();
  const [input, setInput] = useState('');
  const term = useDebounced(input);
  const parent = path[path.length - 1];
  const repo = timesheets.repository();
  const [children, retry] = useLoaded(`children|${parent?.id}`, () =>
    repo?.taskChildren(parent?.id ?? null) ?? Promise.resolve([] as TaskChild[]),
  );
  // Whether the parent itself may be booked on: as the level above has it.
  const [above] = useLoaded(parent ? `children|${path[path.length - 2]?.id}` : null, () =>
    repo?.taskChildren(path[path.length - 2]?.id ?? null) ?? Promise.resolve([] as TaskChild[]),
  );
  const parentBookable =
    parent != null && above?.status === 'data' && above.value.some((it) => it.id === parent.id && it.bookable);
  const taskOf = (id: number, title: string): TaskRef => ({ id, title, parents: path.map((it) => it.title) });

  let body;
  if (term) body = <SearchResults term={term} parentId={parent?.id} current={current} onPick={onPick} />;
  else if (children == null || children.status === 'loading') body = <ActivityIndicator style={styles.fill} />;
  else if (children.status === 'error') body = <ErrorView message={errorText(children.error)} onRetry={retry} />;
  else {
    body = (
      <ScrollView keyboardShouldPersistTaps="handled">
        {parentBookable && (
          <Row
            icon="checkmark-circle-outline"
            label={t('app.timesheet.task.bookOn', parent.title)}
            selected={current?.id === parent.id}
            onPress={() => onPick({ id: parent.id, title: parent.title, parents: path.slice(0, -1).map((it) => it.title) })}
          />
        )}
        {children.value.length === 0 && !parentBookable && (
          <Text style={[styles.hint, { color: colors.text }]}>{t('app.timesheet.task.treeEmpty')}</Text>
        )}
        {children.value.map((it) => {
          const title = it.title ?? `#${it.id}`;
          return (
            <Row
              key={it.id}
              icon={it.hasChildren ? 'folder-outline' : 'document-text-outline'}
              label={title}
              chevron={it.hasChildren}
              disabled={!it.hasChildren && !it.bookable}
              selected={current?.id === it.id}
              onPress={() => (it.hasChildren ? onPath([...path, { id: it.id, title }]) : onPick(taskOf(it.id, title)))}
            />
          );
        })}
      </ScrollView>
    );
  }
  return (
    <SafeAreaView style={[styles.fill, { backgroundColor: colors.background }]}>
      <View style={[styles.bar, { borderColor: colors.border }]}>
        {/* Leaving the tree without a pick closes the picker: the user is back in the form at once. */}
        <Pressable accessibilityLabel={t('back')} hitSlop={8} onPress={() => onPick(null)}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.barTitle, { color: colors.text }]} numberOfLines={1}>
          {parent?.title ?? t('app.timesheet.task.tree')}
        </Text>
      </View>
      {path.length > 0 && (
        <ScrollView horizontal style={styles.crumbs} contentContainerStyle={styles.crumbsContent} showsHorizontalScrollIndicator={false}>
          <Pressable accessibilityLabel={t('app.timesheet.task.tree')} hitSlop={6} onPress={() => onPath([])}>
            <Ionicons name="home-outline" size={20} color={colors.primary} />
          </Pressable>
          {path.slice(0, -1).map((it, i) => (
            <View key={it.id} style={styles.crumb}>
              <Ionicons name="chevron-forward" size={16} color={colors.textMuted} />
              <Pressable hitSlop={6} onPress={() => onPath(path.slice(0, i + 1))}>
                <Text style={{ color: colors.primary }}>{it.title}</Text>
              </Pressable>
            </View>
          ))}
        </ScrollView>
      )}
      <View style={[styles.below, { borderColor: colors.border }]}>
        <Ionicons name="search" size={18} color={colors.textMuted} />
        <TextInput
          key={parent?.id ?? 'top'}
          style={[styles.search, { color: colors.text }]}
          value={input}
          onChangeText={setInput}
          placeholder={t('app.timesheet.task.searchBelow')}
          placeholderTextColor={colors.textMuted}
          autoCorrect={false}
        />
      </View>
      {body}
    </SafeAreaView>
  );
}

function TaskRow({ task, words, selected, onPress }: { task: TaskRef; words?: string[]; selected: boolean; onPress: () => void }) {
  const colors = useColors();
  return (
    <Pressable style={[styles.row, selected && { backgroundColor: colors.surface }]} onPress={onPress}>
      <Ionicons name="folder-outline" size={20} color={colors.textMuted} />
      <View style={styles.fill}>
        <TaskLabel task={task} words={words} />
      </View>
    </Pressable>
  );
}

function Row({
  icon,
  label,
  chevron,
  disabled,
  selected,
  onPress,
}: {
  icon: ComponentProps<typeof Ionicons>['name'];
  label: string;
  chevron?: boolean;
  disabled?: boolean;
  selected?: boolean;
  onPress: () => void;
}) {
  const colors = useColors();
  const color = disabled ? colors.border : colors.text;
  return (
    <Pressable style={[styles.row, selected && { backgroundColor: colors.surface }]} disabled={disabled} onPress={onPress}>
      <Ionicons name={icon} size={20} color={disabled ? colors.border : colors.textMuted} />
      <Text style={[styles.fill, { color, fontSize: 16 }]}>{label}</Text>
      {chevron && <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  bar: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 8, height: 52, borderBottomWidth: StyleSheet.hairlineWidth },
  barTitle: { flex: 1, fontSize: 18, fontWeight: '600' },
  search: { flex: 1, fontSize: 16, paddingVertical: 8 },
  hint: { padding: 16, textAlign: 'center' },
  header: { paddingHorizontal: 16, paddingTop: 16, paddingBottom: 4, fontWeight: '600' },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 12 },
  crumbs: { flexGrow: 0 },
  crumbsContent: { alignItems: 'center', gap: 4, paddingHorizontal: 12, height: 44 },
  crumb: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  below: { flexDirection: 'row', alignItems: 'center', gap: 8, marginHorizontal: 12, marginVertical: 4, paddingHorizontal: 10, borderWidth: StyleSheet.hairlineWidth, borderRadius: 6 },
});
