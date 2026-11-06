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

import DateTimePicker, { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { BOOKING_STEP_MINUTES, floor5 } from '@projectforge/shared/booking-time';
import { getCalendars } from 'expo-localization';
import { useState } from 'react';
import { Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { i18n, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';

export interface DateTimeRequest {
  mode: 'date' | 'time';
  title: string;
  value: Date;
  onDone: (value: Date) => void;
}

/** A date at most a year back or ahead, as in the Flutter app. */
const DATE_RANGE_DAYS = 365;

function dateRange(): { minimumDate: Date; maximumDate: Date } {
  const today = new Date();
  const day = (offset: number) => new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset);
  return { minimumDate: day(-DATE_RANGE_DAYS), maximumDate: day(DATE_RANGE_DAYS) };
}

/**
 * Asks for a time in 5-minute steps (12 or 24 hours as set on the device) or a date: on Android the dialog of the system at once (its time comes rounded
 * down to the step), on iOS the wheels in a sheet ([DateTimeSheet] shows it).
 */
export function pickDateTime(request: DateTimeRequest, showSheet: (request: DateTimeRequest) => void) {
  if (Platform.OS !== 'android') {
    showSheet(request);
    return;
  }
  DateTimePickerAndroid.open({
    value: request.mode === 'time' ? floor5(request.value) : request.value,
    mode: request.mode,
    is24Hour: getCalendars()[0]?.uses24hourClock ?? true,
    ...(request.mode === 'date' ? dateRange() : {}),
    onChange: (event, date) => {
      if (event.type === 'set' && date) request.onDone(request.mode === 'time' ? floor5(date) : date);
    },
  });
}

/** The wheels of iOS for [request] in a sheet; "done" takes the value. */
export function DateTimeSheet({ request, onClose }: { request: DateTimeRequest | null; onClose: () => void }) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const [value, setValue] = useState<{ for: DateTimeRequest; date: Date } | null>(null);
  const current = request == null ? null : value?.for === request ? value.date : floor5(request.value);
  return (
    <Modal visible={request != null} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <View
          style={[styles.sheet, { backgroundColor: colors.surface, paddingBottom: insets.bottom + 8 }]}
          onStartShouldSetResponder={() => true}
        >
          <View style={styles.bar}>
            <Text style={[styles.title, { color: colors.text }]}>{request?.title}</Text>
            <Pressable
              hitSlop={8}
              onPress={() => {
                if (request && current) request.onDone(current);
                onClose();
              }}
            >
              <Text style={[styles.done, { color: colors.primary }]}>{t('app.timesheet.done')}</Text>
            </Pressable>
          </View>
          {request && current && (
            <DateTimePicker
              value={current}
              mode={request.mode}
              display={request.mode === 'time' ? 'spinner' : 'inline'}
              minuteInterval={BOOKING_STEP_MINUTES}
              // The time as set on the device (12/24 hours), the date in the language of the app.
              {...(request.mode === 'date' ? { locale: i18n.locale, ...dateRange() } : {})}
              onChange={(_, date) => date && setValue({ for: request, date })}
            />
          )}
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: { borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingTop: 8 },
  bar: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8 },
  title: { flex: 1, fontSize: 17, fontWeight: '600' },
  done: { fontSize: 17, fontWeight: '600' },
});
