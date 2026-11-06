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
import { useState } from 'react';
import { ActivityIndicator, Alert, FlatList, Image, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { MenuItem, MenuSheet } from '../../core/ui/MenuSheet';
import { HeaderAction, ScreenHeader } from '../../core/ui/ScreenHeader';
import { walletAvailable } from '../../core/wallet/walletPass';
import { addToWallet, shareVCard } from './businessCardActions';
import { BusinessCardInfo, displayName } from './businessCards';
import { businessCards, useBusinessCards } from './businessCardsStore';

/** Loads the cards from the server; a failure is reported in an alert, the shown cards stay. */
async function refreshCards() {
  const error = await businessCards.refresh();
  if (error != null) Alert.alert(errorText(error));
}

/**
 * The own business cards: a tap shows the QR code to scan ([onShowCard]), the menu shares a card or adds it to the
 * Wallet. Cards are created and edited in the web pages of the app, which the link at the end opens ([onCreate]).
 * Port of the Flutter app's BusinessCardsScreen.
 */
export function BusinessCardsScreen({
  onShowCard,
  onCreate,
}: {
  onShowCard: (id: string) => void;
  onCreate: () => void;
}) {
  const colors = useColors();
  const state = useBusinessCards();
  const [refreshing, setRefreshing] = useState(false);
  const [menuOf, setMenuOf] = useState<BusinessCardInfo | null>(null);

  const pullToRefresh = async () => {
    setRefreshing(true);
    await refreshCards();
    setRefreshing(false);
  };

  const menuItems = (info: BusinessCardInfo, walletConfigured: boolean): MenuItem[] => [
    { key: 'share', label: t('app.vCard.share'), icon: 'share-outline', onPress: () => void shareVCard(info.card) },
    ...(walletConfigured && walletAvailable
      ? [{ key: 'wallet', label: t('businessCard.addToWallet'), icon: 'wallet-outline' as const, onPress: () => void addToWallet(info.card) }]
      : []),
  ];

  let body;
  if (state.status === 'loading') {
    body = <ActivityIndicator style={styles.fill} />;
  } else if (state.status === 'error') {
    body = <ErrorView message={errorText(state.error)} onRetry={() => void businessCards.refresh()} />;
  } else {
    const { data, fromCache } = state.cards;
    body = (
      <FlatList
        data={data.cards}
        keyExtractor={(info, index) => info.card.id ?? `${index}`}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void pullToRefresh()} />}
        ListHeaderComponent={
          fromCache ? <Text style={[styles.offline, { color: colors.danger }]}>{t('app.offline')}</Text> : undefined
        }
        ListEmptyComponent={<Text style={[styles.none, { color: colors.text }]}>{t('businessCard.none')}</Text>}
        ListFooterComponent={
          fromCache ? undefined : (
            <Pressable style={styles.create} onPress={onCreate}>
              <Ionicons name="create-outline" size={20} color={colors.primary} />
              <Text style={{ color: colors.primary }}>{t('app.businessCard.createOnDesktop')}</Text>
            </Pressable>
          )
        }
        renderItem={({ item }) => (
          <CardTile
            info={item}
            offline={fromCache}
            onPress={() => item.card.id && onShowCard(item.card.id)}
            onMenu={() => setMenuOf(item)}
          />
        )}
      />
    );
  }
  const walletConfigured = state.status === 'data' && state.cards.data.walletConfigured === true;
  return (
    <View style={[styles.fill, { backgroundColor: colors.background }]}>
      <ScreenHeader title={t('menu.myBusinessCards')}>
        <HeaderAction icon="refresh" label={t('refresh')} onPress={() => void refreshCards()} />
      </ScreenHeader>
      {body}
      <MenuSheet
        visible={menuOf != null}
        items={menuOf ? menuItems(menuOf, walletConfigured) : []}
        onClose={() => setMenuOf(null)}
      />
    </View>
  );
}

function CardTile({
  info,
  offline,
  onPress,
  onMenu,
}: {
  info: BusinessCardInfo;
  offline: boolean;
  onPress: () => void;
  onMenu: () => void;
}) {
  const colors = useColors();
  const { card, preview } = info;
  const subtitle = [displayName(card), card.organization].filter((it): it is string => !!it).join('\n');
  return (
    <Pressable style={[styles.tile, { backgroundColor: colors.surface }]} onPress={onPress}>
      {preview.qrCodeUrl ? (
        // The code on white, also in the dark theme: a scanner needs the contrast.
        <Image source={{ uri: preview.qrCodeUrl }} style={styles.qr} resizeMode="contain" />
      ) : (
        <Ionicons name="qr-code-outline" size={48} color={colors.text} />
      )}
      <View style={styles.fill}>
        <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
          {card.cardName || displayName(card)}
        </Text>
        {subtitle ? <Text style={{ color: colors.textMuted }}>{subtitle}</Text> : null}
      </View>
      {!offline && (
        <Pressable accessibilityLabel={t('app.menu.more')} hitSlop={8} onPress={onMenu}>
          <Ionicons name="ellipsis-vertical" size={20} color={colors.textMuted} />
        </Pressable>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  list: { padding: 12, gap: 8, paddingBottom: 24 },
  offline: { marginBottom: 8 },
  none: { padding: 24, textAlign: 'center' },
  tile: { flexDirection: 'row', alignItems: 'center', gap: 12, borderRadius: 8, padding: 12 },
  qr: { width: 56, height: 56, backgroundColor: '#fff' },
  title: { fontSize: 16, fontWeight: '500' },
  create: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, paddingVertical: 16 },
});
