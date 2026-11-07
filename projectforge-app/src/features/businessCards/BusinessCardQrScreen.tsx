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
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { errorText, t } from '../../core/i18n/translations';
import { HeaderAction } from '../../core/ui/ScreenHeader';
import { walletAvailable } from '../../core/wallet/walletPass';
import { addToWallet, shareVCard } from './businessCardActions';
import { BusinessCardContents } from './BusinessCardContents';
import { displayName } from './businessCards';
import { businessCards, useBusinessCards } from './businessCardsStore';

const PADDING = 24;
const BAR_HEIGHT = 48;

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
 * screen at full brightness, the fields it contains below for control. Works offline (the QR code is part of the cached
 * list, the photo is stored). Port of the Flutter app's BusinessCardQrScreen.
 */
export function BusinessCardQrScreen({ id, onClose }: { id: string; onClose: () => void }) {
  const state = useBusinessCards();
  const { width, height } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  useFullBrightness();
  const info = state.status === 'data' ? state.cards.data.cards.find((it) => it.card.id === id) : undefined;
  const offline = state.status === 'data' && state.cards.fromCache;
  const walletConfigured = state.status === 'data' && state.cards.data.walletConfigured === true;
  const landscape = width > height;
  // Portrait: the texts below the code, all scrolling; landscape: the code as large as the height allows and fixed, the
  // texts beside it scrolling on their own.
  const available = landscape
    ? height - insets.top - insets.bottom - BAR_HEIGHT - 2 * PADDING
    : Math.min(width - 2 * PADDING, height - 2 * PADDING - 176);
  const size = Math.max(80, Math.min(400, available));
  const card = info?.card;
  const qrCode = info?.preview.qrCodeUrl ? (
    <Image source={{ uri: info.preview.qrCodeUrl }} style={{ width: size, height: size }} resizeMode="contain" />
  ) : null;
  const details = info && card && (
    <>
      <Text style={styles.name}>{displayName(card)}</Text>
      {card.organization ? <Text style={styles.organization}>{card.organization}</Text> : null}
      <View style={styles.contents}>
        <BusinessCardContents info={info} />
      </View>
    </>
  );
  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <View style={styles.bar}>
        <HeaderAction icon="arrow-back" label={t('back')} color="#000" onPress={onClose} />
        <Text style={styles.barTitle} numberOfLines={1}>
          {card?.cardName ?? ''}
        </Text>
        {card && !offline && walletConfigured && walletAvailable && (
          <HeaderAction
            icon="wallet-outline"
            label={t('businessCard.addToWallet')}
            color="#000"
            onPress={() => void addToWallet(card)}
          />
        )}
        {card && !offline && (
          <HeaderAction
            icon="share-outline"
            label={t('app.vCard.share')}
            color="#000"
            onPress={() => void shareVCard(card)}
          />
        )}
      </View>
      {info && landscape ? (
        <View style={styles.landscape}>
          {qrCode}
          <ScrollView style={styles.fill} contentContainerStyle={styles.detailsLandscape}>
            {details}
          </ScrollView>
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          {info && (
            <View style={styles.layout}>
              {qrCode}
              <View style={[styles.details, { width: size }]}>{details}</View>
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
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#fff' },
  fill: { flex: 1 },
  bar: { height: BAR_HEIGHT, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8, gap: 4 },
  barTitle: { flex: 1, color: '#000', fontSize: 18, fontWeight: '600', marginLeft: 4 },
  content: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: PADDING },
  layout: { alignItems: 'center', gap: 24 },
  landscape: { flex: 1, flexDirection: 'row', gap: 32, paddingHorizontal: PADDING, paddingTop: PADDING },
  details: { alignItems: 'center' },
  detailsLandscape: { alignItems: 'flex-start', paddingBottom: PADDING },
  name: { color: '#000', fontSize: 22 },
  organization: { color: '#000000a0', fontSize: 16 },
  contents: { alignSelf: 'stretch', marginTop: 16 },
  missing: { alignItems: 'center', gap: 16 },
  retry: { color: '#1976d2', fontSize: 16 },
});
