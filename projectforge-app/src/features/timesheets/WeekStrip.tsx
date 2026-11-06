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
import { addDays, isoWeekday, sameDay, TARGET_MINUTES_PER_DAY, formatDuration } from '@projectforge/shared/booking-time';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';

import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { weekdayShort } from './format';
import { bookingsOf, totalOf, TimesheetWeek } from './timesheet';

/**
 * The days of the week with their totals, a bar measuring them against the working day; a swipe or the arrows show the
 * week before or after.
 */
export function WeekStrip({
  week,
  monday,
  day,
  today,
  onSelect,
}: {
  /** Null while the week is loaded. */
  week: TimesheetWeek | null;
  monday: Date;
  day: Date;
  today: Date;
  onSelect: (day: Date) => void;
}) {
  const colors = useColors();
  const swipe = Gesture.Pan()
    .runOnJS(true)
    .activeOffsetX([-20, 20])
    .onEnd((e) => {
      if (Math.abs(e.velocityX) < 200) return;
      onSelect(addDays(day, e.velocityX < 0 ? 7 : -7));
    });
  return (
    <GestureDetector gesture={swipe}>
      <View style={styles.strip}>
        <Pressable accessibilityLabel={t('app.timesheet.week.previous')} hitSlop={6} onPress={() => onSelect(addDays(day, -7))}>
          <Ionicons name="chevron-back" size={22} color={colors.text} />
        </Pressable>
        {[0, 1, 2, 3, 4, 5, 6].map((i) => {
          const d = addDays(monday, i);
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
        <Pressable accessibilityLabel={t('app.timesheet.week.next')} hitSlop={6} onPress={() => onSelect(addDays(day, 7))}>
          <Ionicons name="chevron-forward" size={22} color={colors.text} />
        </Pressable>
      </View>
    </GestureDetector>
  );
}

const styles = StyleSheet.create({
  strip: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 4, paddingVertical: 6, gap: 2 },
  cell: { flex: 1, alignItems: 'center', paddingVertical: 6, borderRadius: 12 },
  weekday: { fontSize: 11 },
  date: { fontSize: 17, fontWeight: '500' },
  total: { fontSize: 11, marginTop: 2 },
  bar: { width: 24, height: 3, borderRadius: 2, marginTop: 4 },
});
