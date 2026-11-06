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

import * as Brightness from 'expo-brightness';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import {
  ActivityIndicator,
  AppState,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { errorText, t } from '../../core/i18n/translations';
import { HeaderAction } from '../../core/ui/ScreenHeader';
import { walletAvailable } from '../../core/wallet/walletPass';
import { addToWallet, shareVCard } from './businessCardActions';
import { displayName } from './businessCards';
import { businessCards, useBusinessCards } from './businessCardsStore';

const PADDING = 24;

/**
 * Full brightness while the code is shown, the brightness of before afterwards and while the app is in the background
 * (iOS keeps a brightness set by an app until the device is locked; Android only for the app).
 */
function useFullBrightness() {
  useEffect(() => {
    let before: number | null = null;
    const brighten = async () => {
      try {
        before ??= await Brightness.getBrightnessAsync();
        await Brightness.setBrightnessAsync(1);
      } catch {
        // Without the brightness the code is still shown.
      }
    };
    const restore = () => {
      if (before != null) void Brightness.setBrightnessAsync(before).catch(() => undefined);
    };
    void brighten();
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') void brighten();
      else restore();
    });
    return () => {
      subscription.remove();
      restore();
    };
  }, []);
}

/**
 * The QR code of a card in full size, for scanning by somebody else, black on white also in the dark theme, with the
 * screen at full brightness. Works offline (the QR code is part of the cached list). Port of the Flutter app's
 * BusinessCardQrScreen.
 */
export function BusinessCardQrScreen({ id, onClose }: { id: string; onClose: () => void }) {
  const state = useBusinessCards();
  const { width, height } = useWindowDimensions();
  useFullBrightness();
  const info = state.status === 'data' ? state.cards.data.cards.find((it) => it.card.id === id) : undefined;
  const offline = state.status === 'data' && state.cards.fromCache;
  const walletConfigured = state.status === 'data' && state.cards.data.walletConfigured === true;
  const landscape = width > height;
  // Portrait: the texts below the code; landscape: beside it, the code as large as the height allows.
  const available = landscape ? height - 2 * PADDING - 96 : Math.min(width - 2 * PADDING, height - 2 * PADDING - 176);
  const size = Math.max(80, Math.min(400, available));
  const card = info?.card;
  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <View style={styles.bar}>
        <HeaderAction icon="arrow-back" label={t('back')} color="#000" onPress={onClose} />
        <Text style={styles.barTitle} numberOfLines={1}>
          {card?.cardName ?? ''}
        </Text>
        {card && !offline && walletConfigured && walletAvailable && (
          <HeaderAction icon="wallet-outline" label={t('businessCard.addToWallet')} color="#000" onPress={() => void addToWallet(card)} />
        )}
        {card && !offline && (
          <HeaderAction icon="share-outline" label={t('app.vCard.share')} color="#000" onPress={() => void shareVCard(card)} />
        )}
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        {info && card && (
          <View style={[styles.layout, landscape && styles.landscape]}>
            {info.preview.qrCodeUrl ? (
              <Image source={{ uri: info.preview.qrCodeUrl }} style={{ width: size, height: size }} resizeMode="contain" />
            ) : null}
            <View style={landscape ? styles.detailsLandscape : styles.details}>
              <Text style={styles.name}>{displayName(card)}</Text>
              {card.organization ? <Text style={styles.organization}>{card.organization}</Text> : null}
            </View>
          </View>
        )}
        {/* Opened by its id (a restored route): the cards are still loading, or it is gone. */}
        {!info &&
          (state.status === 'loading' ? (
            <ActivityIndicator color="#000" />
          ) : (
            <View style={styles.missing}>
              <Text style={styles.organization}>
                {state.status === 'error' ? errorText(state.error) : t('app.notFound')}
              </Text>
              {state.status === 'error' && (
                <Pressable hitSlop={8} onPress={() => void businessCards.refresh()}>
                  <Text style={styles.retry}>{t('refresh')}</Text>
                </Pressable>
              )}
            </View>
          ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#fff' },
  bar: { height: 48, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8, gap: 4 },
  barTitle: { flex: 1, color: '#000', fontSize: 18, fontWeight: '600', marginLeft: 4 },
  content: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: PADDING },
  layout: { alignItems: 'center', gap: 24 },
  landscape: { flexDirection: 'row' },
  details: { alignItems: 'center' },
  detailsLandscape: { alignItems: 'flex-start', flexShrink: 1 },
  name: { color: '#000', fontSize: 22 },
  organization: { color: '#000000a0', fontSize: 16 },
  missing: { alignItems: 'center', gap: 16 },
  retry: { color: '#1976d2', fontSize: 16 },
});
