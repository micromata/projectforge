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

import { dayOf } from '@projectforge/shared/booking-time';
import { useNavigation, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { View } from 'react-native';

import { AlertBanner } from '../../features/home/AlertBanner';
import { TimesheetsScreen } from '../../features/timesheets/TimesheetsScreen';
import { timesheets } from '../../features/timesheets/timesheetsStore';

/**
 * The time sheets of the user. Opening the tab loads the bookings again; opened on another day than the last time, the
 * screen starts anew, on today.
 */
export default function TimesheetsTab() {
  const router = useRouter();
  const navigation = useNavigation();
  const [today, setToday] = useState(() => dayOf(new Date()).getTime());
  useEffect(
    () =>
      navigation.addListener('focus', () => {
        setToday(dayOf(new Date()).getTime());
        timesheets.reloadWeeks();
      }),
    [navigation],
  );
  return (
    <View style={{ flex: 1 }}>
      <AlertBanner />
      <TimesheetsScreen
        key={today}
        onOpenBooking={() => router.push('/booking')}
        onPickTask={(recent, current) => {
          const picked = timesheets.pickTask(recent, current);
          router.push('/task-picker');
          return picked;
        }}
      />
    </View>
  );
}
