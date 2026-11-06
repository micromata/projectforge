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

import { useRouter } from 'expo-router';
import { View } from 'react-native';

import { BusinessCardsScreen } from '../../features/businessCards/BusinessCardsScreen';
import { AlertBanner } from '../../features/home/AlertBanner';
import { webTabs } from '../../features/web/webTabs';

/** The own business cards; created and edited in the web pages. */
export default function CardsTab() {
  const router = useRouter();
  return (
    <View style={{ flex: 1 }}>
      <AlertBanner />
      <BusinessCardsScreen
        onShowCard={(id) => router.push({ pathname: '/card/[id]', params: { id } })}
        onCreate={() => {
          webTabs.open('pages', '/next/myBusinessCards');
          router.navigate('/pages');
        }}
      />
    </View>
  );
}
