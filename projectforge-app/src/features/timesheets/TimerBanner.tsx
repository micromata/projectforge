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
import { formatDuration, formatTime, two } from '@projectforge/shared/booking-time';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { RunningTimer, shortPath } from './timesheet';

/** The running timer under the week strip: elapsed time, the task if already chosen, stop and discard. */
export function TimerBanner({ timer, onStop, onDiscard }: { timer: RunningTimer; onStop: () => void; onDiscard: () => void }) {
  const colors = useColors();
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);
  const seconds = Math.max(0, Math.floor((now.getTime() - timer.start.getTime()) / 1000));
  const since = formatTime(timer.start);
  const task = timer.what?.task;
  return (
    <View style={[styles.banner, { backgroundColor: colors.surface, borderColor: colors.warning }]}>
      <Ionicons name="timer-outline" size={22} color={colors.warning} />
      <Text style={[styles.elapsed, { color: colors.text }]}>
        {formatDuration(Math.floor(seconds / 60))}:{two(seconds % 60)}
      </Text>
      <Text style={[styles.task, { color: colors.text }]} numberOfLines={1}>
        {task ? t('app.timesheet.timer.since', since, shortPath(task)) : t('app.timesheet.timer.sinceNoTask', since)}
      </Text>
      <Pressable accessibilityLabel={t('app.timesheet.timer.discard')} hitSlop={8} onPress={onDiscard}>
        <Ionicons name="trash-outline" size={20} color={colors.textMuted} />
      </Pressable>
      <Pressable style={[styles.stop, { backgroundColor: colors.warning }]} onPress={onStop}>
        <Ionicons name="stop" size={16} color="#000" />
        <Text style={styles.stopText}>{t('app.timesheet.timer.stop')}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderLeftWidth: 4,
  },
  elapsed: { fontSize: 16, fontWeight: '600', fontVariant: ['tabular-nums'] },
  task: { flex: 1 },
  stop: { flexDirection: 'row', alignItems: 'center', gap: 4, borderRadius: 16, paddingHorizontal: 12, paddingVertical: 6 },
  stopText: { color: '#000', fontWeight: '600' },
});
