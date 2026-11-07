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
  addDays,
  daysBetween,
  formatDuration,
  isoWeekday,
  sameDay,
  TARGET_MINUTES_PER_DAY,
  weekStartOf,
} from '@projectforge/shared/booking-time';
import { memo, useEffect, useMemo, useRef, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { weekdayShort } from './format';
import { bookingsOf, totalOf, TimesheetWeek } from './timesheet';
import { WeekState } from './timesheetsStore';

/**
 * The days of the week with their totals, a bar measuring them against the working day. The weeks lie side by side:
 * the strip follows the finger and snaps to a whole week (starting on the user's first day of the week); the arrows
 * show the week before or after. Another week selects the same weekday in it.
 */
export function WeekStrip({
  weeks,
  firstDay,
  day,
  today,
  rangeDays,
  onSelect,
}: {
  /** The loaded weeks by the time of their first day. */
  weeks: Record<number, WeekState>;
  /** The user's first day of the week, Monday 1 … Sunday 7. */
  firstDay: number;
  day: Date;
  today: Date;
  /** The days before and after today that can be selected. */
  rangeDays: number;
  onSelect: (day: Date) => void;
}) {
  const colors = useColors();
  const [pageWidth, setPageWidth] = useState(0);
  const list = useRef<FlatList<number>>(null);
  const first = addDays(today, -rangeDays);
  const last = addDays(today, rangeDays);
  const base = weekStartOf(first, firstDay);
  const count = daysBetween(base, weekStartOf(last, firstDay)) / 7 + 1;
  const pages = useMemo(() => Array.from({ length: count }, (_, i) => i), [count]);
  const index = daysBetween(base, weekStartOf(day, firstDay)) / 7;
  // The page the list stands on (or scrolls to): a week selected from outside is scrolled to, the own swipe isn't.
  const shown = useRef(index);
  const extraData = useMemo(() => ({ weeks, day }), [weeks, day]);

  /** The same weekday [weeks] later, within the selectable days. */
  const shift = (by: number) => {
    const target = addDays(day, 7 * by);
    onSelect(target < first ? first : target > last ? last : target);
  };

  useEffect(() => {
    if (shown.current === index) return;
    list.current?.scrollToIndex({ index, animated: Math.abs(shown.current - index) === 1 });
    shown.current = index;
  }, [index]);

  return (
    <View style={styles.strip}>
      <Pressable accessibilityLabel={t('app.timesheet.week.previous')} hitSlop={6} onPress={() => shift(-1)}>
        <Ionicons name="chevron-back" size={22} color={colors.text} />
      </Pressable>
      <View style={styles.pager} onLayout={(e) => setPageWidth(e.nativeEvent.layout.width)}>
        {pageWidth > 0 && (
          <FlatList
            // Another first day of the week numbers the pages anew.
            key={`${firstDay}-${pageWidth}`}
            ref={list}
            horizontal
            pagingEnabled
            showsHorizontalScrollIndicator={false}
            data={pages}
            extraData={extraData}
            keyExtractor={(it) => String(it)}
            initialScrollIndex={index}
            getItemLayout={(_, i) => ({ length: pageWidth, offset: pageWidth * i, index: i })}
            windowSize={3}
            initialNumToRender={1}
            maxToRenderPerBatch={2}
            onMomentumScrollEnd={(e) => {
              const target = Math.round(e.nativeEvent.contentOffset.x / pageWidth);
              if (target === shown.current) return;
              const by = target - index;
              shown.current = target;
              if (by !== 0) shift(by);
            }}
            renderItem={({ item }) => {
              const weekStart = addDays(base, 7 * item);
              const state = weeks[weekStart.getTime()];
              return (
                <WeekPage
                  width={pageWidth}
                  weekStart={weekStart}
                  week={state?.status === 'data' ? state.week : null}
                  day={day}
                  today={today}
                  onSelect={onSelect}
                />
              );
            }}
          />
        )}
      </View>
      <Pressable accessibilityLabel={t('app.timesheet.week.next')} hitSlop={6} onPress={() => shift(1)}>
        <Ionicons name="chevron-forward" size={22} color={colors.text} />
      </Pressable>
    </View>
  );
}

/** The seven days of the week starting on [weekStart]; [week] is null while it is loaded. */
const WeekPage = memo(function WeekPage({
  width,
  weekStart,
  week,
  day,
  today,
  onSelect,
}: {
  width: number;
  weekStart: Date;
  week: TimesheetWeek | null;
  day: Date;
  today: Date;
  onSelect: (day: Date) => void;
}) {
  const colors = useColors();
  return (
    <View style={[styles.page, { width }]}>
      {[0, 1, 2, 3, 4, 5, 6].map((i) => {
        const d = addDays(weekStart, i);
        const selected = sameDay(d, day);
        const isToday = sameDay(d, today);
        const total = week ? totalOf(bookingsOf(week, d)) : 0;
        const weekend = isoWeekday(d) > 5;
        const dim = weekend && !selected ? colors.textMuted : colors.text;
        const bar = total >= TARGET_MINUTES_PER_DAY ? colors.primary : total > 0 ? colors.warning : colors.border;
        return (
          <Pressable
            key={i}
            style={[styles.cell, selected && { backgroundColor: colors.surface }]}
            accessibilityState={{ selected }}
            onPress={() => onSelect(d)}
          >
            <Text style={[styles.weekday, { color: dim }]}>{weekdayShort(d)}</Text>
            <Text
              style={[
                styles.date,
                { color: isToday ? colors.primary : dim },
                isToday && { fontWeight: '700', textDecorationLine: 'underline' },
              ]}
            >
              {d.getDate()}
            </Text>
            <Text style={[styles.total, { color: dim }]}>{total === 0 ? '–' : formatDuration(total)}</Text>
            <View style={[styles.bar, { backgroundColor: bar }]} />
          </Pressable>
        );
      })}
    </View>
  );
});

const styles = StyleSheet.create({
  strip: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 4, paddingVertical: 6, gap: 2 },
  pager: { flex: 1 },
  page: { flexDirection: 'row', gap: 2 },
  cell: { flex: 1, alignItems: 'center', paddingVertical: 6, borderRadius: 12 },
  weekday: { fontSize: 11 },
  date: { fontSize: 17, fontWeight: '500' },
  total: { fontSize: 11, marginTop: 2 },
  bar: { width: 24, height: 3, borderRadius: 2, marginTop: 4 },
});
