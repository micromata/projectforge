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

import { useIsFocused, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { View } from 'react-native';

import { AddressesScreen } from '../../features/addresses/AddressesScreen';
import { addresses } from '../../features/addresses/addressesStore';
import { AlertBanner } from '../../features/home/AlertBanner';
import { webTabs } from '../../features/web/webTabs';

/** The address book; addresses are created and edited in the web pages. Synced silently when the tab is opened. */
export default function AddressesTab() {
  const router = useRouter();
  const focused = useIsFocused();
  useEffect(() => {
    if (focused) addresses.refreshIfStale();
  }, [focused]);
  return (
    <View style={{ flex: 1 }}>
      <AlertBanner />
      <AddressesScreen
        onShow={(id) => router.push({ pathname: '/address/[id]', params: { id: String(id) } })}
        onCreate={() => {
          webTabs.open('pages', '/next/address/new');
          router.navigate('/pages');
        }}
      />
    </View>
  );
}
