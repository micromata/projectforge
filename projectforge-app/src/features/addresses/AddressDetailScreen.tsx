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
import { StatusBar } from 'expo-status-bar';
import { ComponentProps, ReactNode, useState } from 'react';
import { Linking, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { i18n, t } from '../../core/i18n/translations';
import { appLog } from '../../core/log/appLog';
import { MenuItem, MenuSheet } from '../../core/ui/MenuSheet';
import { AppPostalAddress, displayName, emails, gradientOf, PhoneNumber, phones, postalLines } from './address';
import { AddressAvatar } from './AddressAvatar';
import { useAddresses } from './addressesStore';

type IconName = ComponentProps<typeof Ionicons>['name'];

function phoneLabel(kind: PhoneNumber['kind']): string {
  switch (kind) {
    case 'business':
      return t('address.phoneType.business.short');
    case 'mobile':
      return t('address.phoneType.mobile.short');
    case 'private':
      return t('address.phoneType.private.short');
    case 'privateMobile':
      return t('address.phoneType.privateMobile.short');
    default:
      return t('address.phoneType.fax.short');
  }
}

/** Opens a number, a mail address (`tel`, `sms`, `mailto`) in the app of the system; only harmless characters. */
function launch(scheme: 'tel' | 'sms' | 'mailto', value: string) {
  open(`${scheme}:${value.replace(/[^0-9+*#@.a-zA-Z_-]/g, '')}`);
}

function launchWebsite(website: string) {
  const trimmed = website.trim();
  open(/^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`);
}

function launchMap(postal: AppPostalAddress) {
  const query = encodeURIComponent(postalLines(postal).join(', '));
  open(Platform.OS === 'ios' ? `https://maps.apple.com/?q=${query}` : `geo:0,0?q=${query}`);
}

function open(url: string) {
  Linking.openURL(url).catch((e) => appLog.warn('addresses', `Not opened: ${String(e)}`));
}

/** `yyyy-MM-dd` in the format of the language. */
function formatBirthday(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!match) return iso;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return date.toLocaleDateString(i18n.locale, { dateStyle: 'medium' });
}

/**
 * An address with its numbers, mail addresses and postal addresses. A number offers a call or a text message, a mail
 * address opens the mail app, a postal address the maps app. [onEdit] shows the form in the web pages; null (offline)
 * hides the button. Port of the Flutter app's AddressDetailScreen.
 */
export function AddressDetailScreen({
  id,
  onClose,
  onEdit,
}: {
  id: number;
  onClose: () => void;
  onEdit: (id: number) => void;
}) {
  const state = useAddresses();
  const [sheet, setSheet] = useState<PhoneNumber | null>(null);
  const address = state.status === 'data' ? state.book.addresses.find((it) => it.id === id) : undefined;
  const offline = state.status === 'data' && state.book.fromCache === true;
  if (address == null) {
    return (
      <SafeAreaView style={[styles.screen, { backgroundColor: '#333' }]}>
        <Bar onClose={onClose} />
      </SafeAreaView>
    );
  }
  const [, color] = gradientOf(address);
  const phoneList = phones(address);
  const mobile = phoneList.find((it) => it.mobile);
  const phone = mobile ?? phoneList[0];
  const email = emails(address)[0];
  const subtitle = [address.positionText, address.division, address.organization]
    .filter((it): it is string => !!it?.trim())
    .join(' · ');
  const postals: [string, AppPostalAddress | null | undefined][] = [
    [t('address.heading.businessAddress'), address.business],
    [t('address.heading.postalAddress'), address.postal],
    [t('address.heading.privateAddress'), address.private],
  ];
  const sheetItems = (number: PhoneNumber): MenuItem[] => [
    { key: 'number', label: number.number, info: true },
    ...(number.kind !== 'fax'
      ? [{ key: 'call', label: t('address.directCall.call'), icon: 'call-outline' as IconName, onPress: () => launch('tel', number.number) }]
      : []),
    ...(number.mobile
      ? [{ key: 'sms', label: t('app.address.message'), icon: 'chatbubble-outline' as IconName, onPress: () => launch('sms', number.number) }]
      : []),
  ];
  return (
    <View style={[styles.screen, { backgroundImage: `linear-gradient(to bottom, ${color}, #1f1f24 70%)` }]}>
      <StatusBar style="light" />
      <SafeAreaView style={styles.screen}>
        <Bar onClose={onClose}>
          {!offline && address.writeAccess && (
            <Pressable hitSlop={8} onPress={() => onEdit(address.id)}>
              <Text style={styles.edit}>{t('edit')}</Text>
            </Pressable>
          )}
        </Bar>
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.center}>
            <AddressAvatar address={address} size={144} full />
          </View>
          <Text style={styles.name}>{displayName(address)}</Text>
          {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
          <View style={styles.quick}>
            <QuickAction icon="chatbubble" label={t('app.address.message')} onPress={mobile && (() => launch('sms', mobile.number))} />
            <QuickAction icon="call" label={t('address.directCall.call')} onPress={phone && (() => launch('tel', phone.number))} />
            <QuickAction icon="mail" label={t('email')} onPress={email && (() => launch('mailto', email.address))} />
          </View>
          {phoneList.length > 0 && (
            <Section>
              {phoneList.map((it) => (
                <Row key={it.kind} label={phoneLabel(it.kind)} value={it.number} icon="call" onPress={() => setSheet(it)} />
              ))}
            </Section>
          )}
          {emails(address).length > 0 && (
            <Section>
              {emails(address).map((it) => (
                <Row
                  key={it.address}
                  label={it.private ? t('address.private') : t('address.business')}
                  value={it.address}
                  icon="mail"
                  onPress={() => launch('mailto', it.address)}
                />
              ))}
            </Section>
          )}
          {address.website?.trim() ? (
            <Section>
              <Row
                label={t('address.website')}
                value={address.website.trim()}
                icon="globe-outline"
                onPress={() => launchWebsite(address.website ?? '')}
              />
            </Section>
          ) : null}
          {postals.map(([label, postal]) =>
            postal && postalLines(postal).length > 0 ? (
              <Section key={label}>
                <Row label={label} value={postalLines(postal).join('\n')} icon="map-outline" onPress={() => launchMap(postal)} />
              </Section>
            ) : null,
          )}
          {address.birthday ? (
            <Section>
              <Row label={t('address.birthday')} value={formatBirthday(address.birthday)} />
            </Section>
          ) : null}
          {address.comment?.trim() ? (
            <Section>
              <Row label={t('comment')} value={address.comment.trim()} />
            </Section>
          ) : null}
        </ScrollView>
      </SafeAreaView>
      <MenuSheet visible={sheet != null} items={sheet ? sheetItems(sheet) : []} onClose={() => setSheet(null)} />
    </View>
  );
}

function Bar({ onClose, children }: { onClose: () => void; children?: ReactNode }) {
  return (
    <View style={styles.bar}>
      <Pressable accessibilityLabel={t('back')} hitSlop={8} onPress={onClose}>
        <Ionicons name="chevron-back" size={28} color="#fff" />
      </Pressable>
      <View style={styles.fill} />
      {children}
    </View>
  );
}

function QuickAction({ icon, label, onPress }: { icon: IconName; label: string; onPress?: (() => void) | false }) {
  const enabled = typeof onPress === 'function';
  return (
    <Pressable
      accessibilityLabel={label}
      disabled={!enabled}
      onPress={enabled ? onPress : undefined}
      style={[styles.quickAction, { backgroundColor: `rgba(255,255,255,${enabled ? 0.2 : 0.08})` }]}
    >
      <Ionicons name={icon} size={24} color={`rgba(255,255,255,${enabled ? 1 : 0.4})`} />
    </Pressable>
  );
}

/** A group of rows on a translucent card. */
function Section({ children }: { children: ReactNode }) {
  return <View style={styles.section}>{children}</View>;
}

function Row({ label, value, icon, onPress }: { label: string; value: string; icon?: IconName; onPress?: () => void }) {
  return (
    <Pressable style={styles.row} onPress={onPress} disabled={onPress == null}>
      <View style={styles.fill}>
        <Text style={styles.rowLabel}>{label}</Text>
        <Text style={styles.rowValue} selectable>
          {value}
        </Text>
      </View>
      {icon && <Ionicons name={icon} size={18} color="#fff" />}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  fill: { flex: 1 },
  bar: { height: 44, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8 },
  edit: { color: '#fff', fontSize: 17, paddingHorizontal: 8 },
  content: { paddingHorizontal: 16, paddingBottom: 32 },
  center: { alignItems: 'center' },
  name: { color: '#fff', fontSize: 24, fontWeight: '600', textAlign: 'center', marginTop: 16 },
  subtitle: { color: 'rgba(255,255,255,0.7)', textAlign: 'center', marginTop: 4 },
  quick: { flexDirection: 'row', justifyContent: 'center', gap: 12, marginVertical: 16 },
  quickAction: { width: 52, height: 52, borderRadius: 26, alignItems: 'center', justifyContent: 'center' },
  section: { backgroundColor: 'rgba(255,255,255,0.14)', borderRadius: 20, marginBottom: 10, overflow: 'hidden' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(255,255,255,0.15)',
  },
  rowLabel: { color: 'rgba(255,255,255,0.7)' },
  rowValue: { color: '#fff', fontSize: 16, marginTop: 2 },
});
