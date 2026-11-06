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

import { Image, StyleSheet, Text, View } from 'react-native';

import { AppAddress, gradientOf, initials } from './address';
import { useAddressImage } from './addressesStore';

/**
 * The thumbnail of an address, or its initials on a gradient (as the Contacts app of Apple). [full]: the image itself
 * (the thumbnail until it is there), for a large avatar.
 */
export function AddressAvatar({ address, size, full = false }: { address: AppAddress; size: number; full?: boolean }) {
  const uri = useAddressImage(address, full);
  const [top, bottom] = gradientOf(address);
  const round = { width: size, height: size, borderRadius: size / 2 };
  return (
    <View style={[styles.avatar, round, { backgroundImage: `linear-gradient(to bottom, ${top}, ${bottom})` }]}>
      {uri ? (
        <Image source={{ uri }} style={round} resizeMode="cover" />
      ) : (
        <Text style={[styles.initials, { fontSize: size * 0.4 }]}>{initials(address)}</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  avatar: { alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  initials: { color: '#fff', fontWeight: '500' },
});
