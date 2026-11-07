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

import { useLocalSearchParams, useRouter } from 'expo-router';

import { AddressDetailScreen } from '../../features/addresses/AddressDetailScreen';
import { webTabs } from '../../features/web/webTabs';
import { useClose } from '../_layout';

/** An address; editing happens in the web pages. */
export default function Address() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  return (
    <AddressDetailScreen
      id={Number(id)}
      onClose={useClose()}
      onEdit={(addressId) => {
        webTabs.open('pages', `/next/address/${addressId}/edit`);
        router.dismissTo('/pages');
      }}
    />
  );
}
