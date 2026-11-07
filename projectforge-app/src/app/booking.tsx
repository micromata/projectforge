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

import { weekStartOf } from '@projectforge/shared/booking-time';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useFirstDayOfWeek } from '../core/status/userStatus';
import { useColors } from '../core/ui/colors';
import { BookingForm } from '../features/timesheets/BookingForm';
import { bookingDone } from '../features/timesheets/TimesheetsScreen';
import { BookingStart, timesheets, useTimesheets } from '../features/timesheets/timesheetsStore';
import { useClose } from './_layout';

/** The booking form on a phone, over the tabs (a tablet shows it beside the list). */
export default function Booking() {
  const router = useRouter();
  const close = useClose();
  const colors = useColors();
  const state = useTimesheets();
  const firstDay = useFirstDayOfWeek();
  const [start] = useState<BookingStart | null>(() => timesheets.takeBooking());
  useEffect(() => {
    // Restored without its start (e.g. after a reload of the bundle): nothing to show.
    if (start == null) close();
  });
  if (start == null) return null;
  const week = state.weeks[weekStartOf(start.start, firstDay).getTime()];
  const sheets = week?.status === 'data' ? week.week : null;
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.background }} edges={['top', 'left', 'right']}>
      <BookingForm
        initial={start}
        tags={sheets?.tags ?? []}
        onPickTask={(recent, current) => {
          const picked = timesheets.pickTask(recent, current);
          router.push('/task-picker');
          return picked;
        }}
        onDone={(saved) => {
          close();
          bookingDone(saved, start.fromTimer === true);
        }}
        onDelete={
          start.id == null
            ? undefined
            : () => {
                close();
                timesheets.requestDelete(start.id!);
              }
        }
      />
    </SafeAreaView>
  );
}
