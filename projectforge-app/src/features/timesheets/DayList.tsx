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
import {
  formatDuration,
  formatTime,
  isoWeekday,
  TARGET_MINUTES_PER_DAY,
} from '@projectforge/shared/booking-time';
import { useRef, useState } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import ReanimatedSwipeable, { SwipeableMethods, SwipeDirection } from 'react-native-gesture-handler/ReanimatedSwipeable';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { dayLong } from './format';
import {
  acceptancePending,
  bookingsOf,
  durationOf,
  Gap,
  gapsOf,
  nextStartOf,
  shortPath,
  Timesheet,
  totalOf,
} from './timesheet';
import { WeekState } from './timesheetsStore';

export interface DayActions {
  open: (sheet: Timesheet) => void;
  /** Book the period (a gap, or from the next start of the day). */
  book: (start: Date, stop: Date) => void;
  duplicate: (sheet: Timesheet) => void;
  delete: (sheet: Timesheet) => void;
  menu: (sheet: Timesheet) => void;
  refresh: () => Promise<void>;
  retry: () => void;
}

/** The bookings of a day with the gaps between them; a swipe to the right duplicates, to the left deletes. */
export function DayList({
  day,
  week,
  hidden,
  selectedId,
  actions,
}: {
  day: Date;
  week: WeekState | undefined;
  /** Deleted, but still in the list the server answered last. */
  hidden: Set<number>;
  /** The booking shown in the side panel. */
  selectedId?: number;
  actions: DayActions;
}) {
  const colors = useColors();
  const [refreshing, setRefreshing] = useState(false);
  if (week == null || week.status === 'loading') return <ActivityIndicator style={styles.fill} />;
  if (week.status === 'error') return <ErrorView message={errorText(week.error)} onRetry={actions.retry} />;
  const data = week.week;
  const bookings = bookingsOf(data, day).filter((it) => !hidden.has(it.id));
  const total = totalOf(bookings);
  const items: (Timesheet | (Gap & { gap: true }))[] = [
    ...bookings,
    ...gapsOf(data, day, new Date()).map((it) => ({ ...it, gap: true as const })),
  ].sort((a, b) => a.start.getTime() - b.start.getTime());
  const refresh = async () => {
    setRefreshing(true);
    await actions.refresh();
    setRefreshing(false);
  };
  return (
    <ScrollView
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void refresh()} />}
    >
      {data.fromCache && (
        <View style={styles.offline}>
          <Ionicons name="cloud-offline-outline" size={18} color={colors.textMuted} />
          <Text style={[styles.fill, { color: colors.text }]}>{t('app.timesheet.offline')}</Text>
        </View>
      )}
      <View style={styles.dayHeader}>
        <Text style={[styles.dayTitle, { color: colors.text }]}>{dayLong(day)}</Text>
        <Text style={[styles.dayTitle, { color: colors.text }]}>Σ {formatDuration(total)} h</Text>
      </View>
      <View style={[styles.progress, { backgroundColor: colors.border }]}>
        <View
          style={[
            styles.progressValue,
            { backgroundColor: colors.primary, width: `${Math.min(100, (total / TARGET_MINUTES_PER_DAY) * 100)}%` },
          ]}
        />
      </View>
      {items.length === 0 && (
        <View style={styles.empty}>
          <Ionicons name="calendar-outline" size={48} color={colors.textMuted} />
          <Text style={{ color: colors.text }}>
            {isoWeekday(day) > 5 ? t('app.timesheet.emptyWeekend') : t('app.timesheet.empty')}
          </Text>
          <Pressable
            style={[styles.emptyButton, { backgroundColor: colors.surface }]}
            onPress={() => {
              const start = nextStartOf(data, day);
              actions.book(start, new Date(start.getTime() + 3_600_000));
            }}
          >
            <Ionicons name="add" size={18} color={colors.primary} />
            <Text style={{ color: colors.primary }}>{t('app.timesheet.bookFrom', formatTime(nextStartOf(data, day)))}</Text>
          </Pressable>
        </View>
      )}
      {items.map((item) =>
        'gap' in item ? (
          <GapRow key={`gap-${item.start.getTime()}`} gap={item} onPress={() => actions.book(item.start, item.stop)} />
        ) : (
          <BookingCard key={item.id} sheet={item} selected={item.id === selectedId} actions={actions} />
        ),
      )}
    </ScrollView>
  );
}

function BookingCard({ sheet, selected, actions }: { sheet: Timesheet; selected: boolean; actions: DayActions }) {
  const colors = useColors();
  const swipeable = useRef<SwipeableMethods>(null);
  const { what } = sheet;
  const extra = [what.location, what.tag].filter((it): it is string => !!it).join(' · ');
  const background = (icon: 'copy-outline' | 'trash-outline', label: string, color: string, right: boolean) => (
    <View style={[styles.swipe, { backgroundColor: color, justifyContent: right ? 'flex-end' : 'flex-start' }]}>
      <Ionicons name={icon} size={20} color="#fff" />
      <Text style={styles.swipeText}>{label}</Text>
    </View>
  );
  return (
    <ReanimatedSwipeable
      ref={swipeable}
      friction={2}
      leftThreshold={80}
      rightThreshold={80}
      containerStyle={styles.cardContainer}
      renderLeftActions={() => background('copy-outline', t('app.timesheet.duplicate'), colors.primary, false)}
      renderRightActions={() => background('trash-outline', t('delete'), colors.danger, true)}
      onSwipeableOpen={(direction) => {
        swipeable.current?.close();
        // The content moved to the right: the actions of the left side.
        if (direction === SwipeDirection.RIGHT) actions.duplicate(sheet);
        else actions.delete(sheet);
      }}
    >
      <Pressable
        style={[styles.card, { backgroundColor: selected ? colors.border : colors.surface }]}
        onPress={() => actions.open(sheet)}
        onLongPress={() => actions.menu(sheet)}
      >
        <View style={styles.times}>
          <Text style={[styles.start, { color: colors.text }]}>{formatTime(sheet.start)}</Text>
          <Text style={{ color: colors.textMuted, fontSize: 12 }}>{formatTime(sheet.stop)}</Text>
        </View>
        <View style={styles.fill}>
          <Text style={[styles.task, { color: colors.text }]}>{what.task ? shortPath(what.task) : ''}</Text>
          {what.kost2?.label ? <Text style={{ color: colors.textMuted, fontSize: 12 }}>{what.kost2.label}</Text> : null}
          {what.description ? (
            <Text style={{ color: colors.text }} numberOfLines={2}>
              {what.description}
            </Text>
          ) : null}
          {extra ? <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 2 }}>{extra}</Text> : null}
          {acceptancePending(sheet) && (
            <Text style={{ color: colors.warning, fontSize: 11, marginTop: 2 }}>{t('app.timesheet.acceptancePending')}</Text>
          )}
        </View>
        <Text style={[styles.duration, { color: colors.text }]}>{formatDuration(durationOf(sheet))}</Text>
      </Pressable>
    </ReanimatedSwipeable>
  );
}

function GapRow({ gap, onPress }: { gap: Gap; onPress: () => void }) {
  const colors = useColors();
  return (
    <Pressable style={styles.gap} onPress={onPress}>
      <View style={[styles.gapLine, { width: 16, backgroundColor: colors.border }]} />
      <Ionicons name="add-circle-outline" size={18} color={colors.primary} />
      <Text style={{ color: colors.primary, fontSize: 12, flexShrink: 1 }} numberOfLines={1}>
        {t('app.timesheet.gap', formatTime(gap.start), formatTime(gap.stop), formatDuration(durationOf(gap)))}
      </Text>
      <View style={[styles.gapLine, styles.fill, { backgroundColor: colors.border }]} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  content: { paddingBottom: 160 },
  offline: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingTop: 8 },
  dayHeader: { flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: 16, paddingTop: 16, paddingBottom: 4 },
  dayTitle: { fontSize: 16, fontWeight: '600' },
  progress: { height: 6, borderRadius: 3, marginHorizontal: 16, marginVertical: 6, overflow: 'hidden' },
  progressValue: { height: 6 },
  empty: { alignItems: 'center', gap: 12, paddingHorizontal: 32, paddingTop: 48 },
  emptyButton: { flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: 20, paddingHorizontal: 16, paddingVertical: 10 },
  cardContainer: { marginHorizontal: 12, marginVertical: 4, borderRadius: 12 },
  card: { flexDirection: 'row', gap: 8, padding: 12, borderRadius: 12 },
  times: { width: 52 },
  start: { fontSize: 14, fontWeight: '600' },
  task: { fontSize: 14, fontWeight: '600' },
  duration: { fontSize: 16, fontWeight: '600' },
  swipe: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 20, borderRadius: 12 },
  swipeText: { color: '#fff', fontWeight: '600' },
  gap: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 16, paddingVertical: 6 },
  gapLine: { height: StyleSheet.hairlineWidth },
});
