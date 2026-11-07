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

import { ReactNode } from 'react';
import { ActivityIndicator, Image, StyleSheet, Text, View } from 'react-native';

import { formatIsoDay } from '../../core/i18n/dates';
import { t } from '../../core/i18n/translations';
import { BusinessCardInfo, filledFields } from './businessCards';
import { useCardPhoto } from './businessCardsStore';

/**
 * The small control list below the QR code: every field (and the photo) the card really contains, as in the web
 * version (`business-card-contents.tsx`). Black on white, like the code. The photo is the stored one without network.
 */
export function BusinessCardContents({ info }: { info: BusinessCardInfo }) {
  const { card, preview } = info;
  const rows: { key: string; label: string; value: ReactNode }[] = filledFields(card).map(
    ({ field, labelKey, value }) => ({
      key: field,
      label: t(labelKey),
      value: <Text style={styles.value}>{field === 'birthday' ? formatIsoDay(value) : value}</Text>,
    }),
  );
  if (preview.withPhoto) {
    const label = t('businessCard.includePhoto');
    rows.push({
      key: 'photo',
      label,
      value:
        card.addressId != null ? (
          <Photo addressId={card.addressId} label={label} />
        ) : (
          <Text style={styles.value}>✓</Text>
        ),
    });
  }
  if (rows.length === 0) return null;
  return (
    <View style={styles.list}>
      {rows.map(({ key, label, value }) => (
        <View key={key} style={styles.row}>
          <Text style={styles.label}>{label}</Text>
          <View style={styles.cell}>{value}</View>
        </View>
      ))}
    </View>
  );
}

/** The small photo of the card; if there is none, the row says so instead of showing a broken image. */
function Photo({ addressId, label }: { addressId: number; label: string }) {
  const uri = useCardPhoto(addressId);
  if (uri === undefined) return <ActivityIndicator color="#000" style={styles.loading} />;
  if (uri === null) return <Text style={styles.unavailable}>{t('app.businessCard.photoUnavailable')}</Text>;
  return <Image source={{ uri }} accessibilityLabel={label} style={styles.photo} resizeMode="cover" />;
}

const styles = StyleSheet.create({
  list: { gap: 2, alignSelf: 'stretch' },
  row: { flexDirection: 'row', gap: 8 },
  label: { color: '#000000a0', fontSize: 12, width: 112 },
  cell: { flex: 1 },
  value: { color: '#000', fontSize: 12 },
  unavailable: { color: '#ba1a1a', fontSize: 12 },
  loading: { alignSelf: 'flex-start' },
  photo: {
    width: 48,
    height: 48,
    borderRadius: 6,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#00000040',
  },
});
