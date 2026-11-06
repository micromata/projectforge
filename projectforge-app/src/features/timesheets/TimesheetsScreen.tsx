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
  addMinutes,
  BOOKING_STEP_MINUTES,
  ceil5,
  dayOf,
  daysBetween,
  floor5,
  formatDuration,
  formatTime,
  mondayOf,
  sameDay,
  weekOfYear,
} from '@projectforge/shared/booking-time';
import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { MenuSheet } from '../../core/ui/MenuSheet';
import { snackbar } from '../../core/ui/snackbar';
import { BookingForm } from './BookingForm';
import { DayActions, DayList } from './DayList';
import { monthYear } from './format';
import { TimerBanner } from './TimerBanner';
import { WeekStrip } from './WeekStrip';
import { BookingDraft, BookingWhat, durationOf, nextStartOf, shortPath, Timesheet, TaskRef } from './timesheet';
import { BookingStart, timesheets, useTimesheets } from './timesheetsStore';

/** The days the pager reaches before and after today. */
const RANGE_DAYS = 3650;
/** From this width the form stands beside the list instead of covering it. */
const WIDE_LAYOUT = 840;

/**
 * The time sheets: a week strip with the daily totals, the bookings of the selected day (swiped through) with the gaps
 * between them, "book" and the timer. Wide screens show the form as a side panel instead of a screen of its own.
 * Port of the Flutter app's TimesheetsScreen.
 *
 * @param onOpenBooking Shows the booking form as a screen of its own (phones).
 * @param onPickTask Shows the task picker; resolves with the chosen task.
 */
export function TimesheetsScreen({
  onOpenBooking,
  onPickTask,
}: {
  onOpenBooking: () => void;
  onPickTask: (recent: TaskRef[], current: TaskRef | null) => Promise<TaskRef | null>;
}) {
  const colors = useColors();
  const { width } = useWindowDimensions();
  const wide = width >= WIDE_LAYOUT;
  const state = useTimesheets();
  // The screen is created anew when the tab is opened on another day (see the route): back to today.
  const [today] = useState(() => dayOf(new Date()));
  const [day, setDay] = useState(today);
  const [pageWidth, setPageWidth] = useState(0);
  const [hidden, setHidden] = useState<Set<number>>(new Set());
  const [panel, setPanel] = useState<{ start: BookingStart; key: number } | null>(null);
  const [menuOf, setMenuOf] = useState<Timesheet | null>(null);
  const pager = useRef<FlatList<number>>(null);

  const monday = mondayOf(day);
  const weekState = state.weeks[monday.getTime()];
  const week = weekState?.status === 'data' ? weekState.week : null;

  // The week of the day and the ones around it (swiping across Sunday or Monday shows them at once).
  useEffect(() => {
    timesheets.loadWeek(monday);
    timesheets.loadWeek(addDays(monday, -7));
    timesheets.loadWeek(addDays(monday, 7));
  }, [monday.getTime()]); // eslint-disable-line react-hooks/exhaustive-deps

  const select = useCallback(
    (target: Date) => {
      const index = RANGE_DAYS + daysBetween(today, target);
      setDay(dayOf(target));
      pager.current?.scrollToIndex({ index, animated: Math.abs(daysBetween(day, target)) === 1 });
    },
    [today, day],
  );

  // A booking saved on another day (its date changed in the form): that day is shown.
  useEffect(() => {
    timesheets.setDayHandler((target) => {
      if (!sameDay(target, day) && Math.abs(daysBetween(today, target)) <= RANGE_DAYS) select(target);
    });
    return () => timesheets.setDayHandler(null);
  }, [day, today, select]);

  const open = (start: BookingStart) => {
    if (wide) {
      setPanel((it) => ({ start, key: (it?.key ?? 0) + 1 }));
      return;
    }
    timesheets.requestBooking(start);
    onOpenBooking();
  };

  const remove = async (sheet: Timesheet) => {
    const repo = timesheets.repository();
    if (repo == null) return;
    setHidden((it) => new Set(it).add(sheet.id));
    if (panel?.start.id === sheet.id) setPanel(null);
    const show = () =>
      setHidden((it) => {
        const next = new Set(it);
        next.delete(sheet.id);
        return next;
      });
    try {
      const data = await repo.delete(sheet.id);
      timesheets.reloadWeeks();
      snackbar.show({
        text: t('app.timesheet.deleted', `${formatTime(sheet.start)}–${formatTime(sheet.stop)}`, sheet.what.task ? shortPath(sheet.what.task) : ''),
        action: {
          label: t('app.timesheet.undo'),
          onPress: () => {
            repo
              .undelete(data)
              .catch((e: unknown) => snackbar.show(errorText(e)))
              .finally(() => {
                show();
                timesheets.reloadWeeks();
              });
          },
        },
      });
    } catch (e) {
      show();
      snackbar.show(errorText(e));
    }
  };

  // The booking form of a phone deletes through the list, for the undo.
  const removeRef = useRef(remove);
  useEffect(() => {
    removeRef.current = remove;
  });
  useEffect(() => {
    timesheets.setDeleteHandler((id) => {
      const sheet = Object.values(timesheets.getState().weeks)
        .flatMap((it) => (it.status === 'data' ? it.week.sheets : []))
        .find((it) => it.id === id);
      if (sheet) void removeRef.current(sheet);
    });
    return () => timesheets.setDeleteHandler(null);
  }, []);

  const duplicate = (sheet: Timesheet) => {
    const start = week ? nextStartOf(week, day) : sheet.stop;
    open({ what: sheet.what, start, stop: addMinutes(start, durationOf(sheet)) });
  };

  const startTimer = (what: BookingWhat | null = null) => {
    timesheets.startTimer(what);
    snackbar.show(t('app.timesheet.timer.started'));
  };

  const stopTimer = () => {
    const timer = state.timer;
    if (timer == null) return;
    const start = floor5(timer.start);
    let stop = ceil5(new Date());
    if (stop <= start) stop = addMinutes(start, BOOKING_STEP_MINUTES);
    open({ what: timer.what, start, stop, fromTimer: true });
  };

  const newBooking = () => {
    const start = nextStartOf(week, day);
    open({ start, stop: addMinutes(start, 60) });
  };

  const actions: DayActions = {
    open: (sheet) => open({ id: sheet.id, what: sheet.what, start: sheet.start, stop: sheet.stop }),
    book: (start, stop) => open({ start, stop }),
    duplicate,
    delete: (sheet) => void remove(sheet),
    menu: setMenuOf,
    refresh: () => timesheets.reloadWeeks(),
    retry: () => timesheets.loadWeek(monday, true),
  };

  const main = (
    <View style={styles.fill}>
      <View style={styles.titleRow}>
        <View style={styles.fill}>
          <Text style={[styles.title, { color: colors.text }]}>
            {t('app.timesheet.weekTitle', weekOfYear(day), monthYear(day))}
          </Text>
          {week && (
            <Text style={{ color: colors.textMuted, fontSize: 12 }}>
              {t('app.timesheet.weekTotal', formatDuration(week.sheets.reduce((sum, it) => sum + durationOf(it), 0)))}
            </Text>
          )}
        </View>
        {!sameDay(day, today) && (
          <Pressable hitSlop={8} onPress={() => select(today)}>
            <Text style={{ color: colors.primary, fontWeight: '600' }}>{t('calendar.today')}</Text>
          </Pressable>
        )}
      </View>
      <WeekStrip week={week} monday={monday} day={day} today={today} onSelect={select} />
      {state.timer && (
        <TimerBanner timer={state.timer} onStop={stopTimer} onDiscard={() => timesheets.stopTimer()} />
      )}
      <View style={styles.fill} onLayout={(e) => setPageWidth(e.nativeEvent.layout.width)}>
        {pageWidth > 0 && (
          <FlatList
            ref={pager}
            horizontal
            pagingEnabled
            showsHorizontalScrollIndicator={false}
            data={PAGES}
            keyExtractor={(it) => String(it)}
            initialScrollIndex={RANGE_DAYS + daysBetween(today, day)}
            getItemLayout={(_, index) => ({ length: pageWidth, offset: pageWidth * index, index })}
            windowSize={3}
            initialNumToRender={1}
            maxToRenderPerBatch={1}
            onMomentumScrollEnd={(e) => {
              const index = Math.round(e.nativeEvent.contentOffset.x / pageWidth);
              const target = addDays(today, index - RANGE_DAYS);
              if (!sameDay(target, day)) setDay(target);
            }}
            renderItem={({ item }) => {
              const pageDay = addDays(today, item - RANGE_DAYS);
              return (
                <View style={{ width: pageWidth }}>
                  <DayList
                    day={pageDay}
                    week={state.weeks[mondayOf(pageDay).getTime()]}
                    hidden={hidden}
                    selectedId={wide ? panel?.start.id : undefined}
                    actions={actions}
                  />
                </View>
              );
            }}
          />
        )}
      </View>
      <View style={styles.fabs} pointerEvents="box-none">
        {state.timer == null && (
          <Pressable
            style={[styles.fabSmall, { backgroundColor: colors.surface }]}
            accessibilityLabel={t('app.timesheet.timer.start')}
            onPress={() => startTimer()}
          >
            <Ionicons name="timer-outline" size={22} color={colors.primary} />
          </Pressable>
        )}
        <Pressable style={[styles.fab, { backgroundColor: colors.primary }]} onPress={newBooking}>
          <Ionicons name="add" size={22} color={colors.onPrimary} />
          <Text style={[styles.fabText, { color: colors.onPrimary }]}>{t('app.timesheet.book')}</Text>
        </Pressable>
      </View>
      <MenuSheet
        visible={menuOf != null}
        items={
          menuOf
            ? [
                {
                  key: 'info',
                  label: `${menuOf.what.task ? shortPath(menuOf.what.task) : ''}\n${formatTime(menuOf.start)}–${formatTime(menuOf.stop)} · ${menuOf.what.description}`,
                  info: true,
                },
                { key: 'edit', label: t('edit'), icon: 'create-outline', onPress: () => actions.open(menuOf) },
                { key: 'duplicate', label: t('app.timesheet.duplicate.next'), icon: 'copy-outline', onPress: () => duplicate(menuOf) },
                {
                  key: 'continue',
                  label: t('app.timesheet.continue'),
                  icon: 'timer-outline',
                  disabled: state.timer != null,
                  onPress: () => startTimer(menuOf.what),
                },
                { key: 'delete', label: t('delete'), icon: 'trash-outline', destructive: true, onPress: () => void remove(menuOf) },
              ]
            : []
        }
        onClose={() => setMenuOf(null)}
      />
    </View>
  );

  if (!wide) return main;
  return (
    <View style={styles.wide}>
      {main}
      <View style={[styles.panel, { borderColor: colors.border }]}>
        {panel == null ? (
          <Text style={[styles.panelHint, { color: colors.textMuted }]}>{t('app.timesheet.panel.hint')}</Text>
        ) : (
          <BookingForm
            key={panel.key}
            initial={panel.start}
            tags={week?.tags ?? []}
            onPickTask={onPickTask}
            onDone={(saved) => {
              setPanel(null);
              bookingDone(saved, panel.start.fromTimer === true);
            }}
            onDelete={
              panel.start.id == null
                ? undefined
                : () => {
                    const sheet = week?.sheets.find((it) => it.id === panel.start.id);
                    if (sheet) void remove(sheet);
                  }
            }
          />
        )}
      </View>
    </View>
  );
}

/** After the form: the timer ends if it was stopped into it, the message of what was booked. */
export function bookingDone(saved: BookingDraft | null, fromTimer: boolean) {
  if (saved == null) return;
  if (fromTimer) timesheets.stopTimer();
  timesheets.showDay(saved.start);
  snackbar.show(
    t(
      'app.timesheet.booked',
      `${formatTime(saved.start)}–${formatTime(saved.stop)}`,
      formatDuration(durationOf(saved)),
      shortPath(saved.task),
    ),
  );
}

const PAGES = Array.from({ length: 2 * RANGE_DAYS + 1 }, (_, i) => i);

const styles = StyleSheet.create({
  fill: { flex: 1 },
  wide: { flex: 1, flexDirection: 'row' },
  panel: { width: 440, borderLeftWidth: StyleSheet.hairlineWidth },
  panelHint: { padding: 32, textAlign: 'center', fontSize: 16 },
  titleRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingTop: 8 },
  title: { fontSize: 16, fontWeight: '600' },
  fabs: { position: 'absolute', right: 16, bottom: 16, alignItems: 'flex-end', gap: 12 },
  fabSmall: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  fab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    height: 52,
    borderRadius: 26,
    paddingHorizontal: 20,
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  fabText: { fontSize: 16, fontWeight: '600' },
});
