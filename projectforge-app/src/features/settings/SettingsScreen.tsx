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

import { Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';

import { t } from '../../core/i18n/translations';
import { appSettings, opensInApp, RailSide, useAppSettings } from '../../core/settings/appSettings';
import { useColors } from '../../core/ui/colors';
import { NativePage, NATIVE_PAGES } from '../../core/web/nativePage';

/** The label of the menu entry of the web pages that leads to [page]. */
function pageLabel(page: NativePage): string {
  switch (page) {
    case 'chat':
      return t('chat.title');
    case 'businessCards':
      return t('menu.myBusinessCards');
    case 'notifications':
      return t('notification.center');
    case 'addresses':
      return t('menu.addressList');
  }
}

const RAIL_SIDES: { side: RailSide; label: () => string }[] = [
  { side: 'left', label: () => t('app.settings.railSide.left') },
  { side: 'right', label: () => t('app.settings.railSide.right') },
];

/**
 * The settings of the app on this device: the side of the navigation in landscape, which menu entries of the web pages
 * open the app's tab, the log.
 */
export function SettingsScreen({ onShowLog }: { onShowLog: () => void }) {
  const colors = useColors();
  const settings = useAppSettings();
  return (
    <ScrollView style={{ backgroundColor: colors.background }} contentContainerStyle={styles.container}>
      <View style={[styles.row, { borderColor: colors.border }]}>
        <View style={styles.fill}>
          <Text style={[styles.label, { color: colors.text }]}>{t('app.settings.railSide')}</Text>
          <Text style={[styles.info, styles.rowInfo, { color: colors.textMuted }]}>
            {t('app.settings.railSide.info')}
          </Text>
        </View>
        <View style={[styles.segments, { borderColor: colors.border }]} accessibilityRole="radiogroup">
          {RAIL_SIDES.map((it) => {
            const selected = it.side === settings.railSide;
            return (
              <Pressable
                key={it.side}
                style={[styles.segment, selected && { backgroundColor: colors.primary }]}
                accessibilityRole="radio"
                accessibilityState={{ selected }}
                onPress={() => appSettings.setRailSide(it.side)}
              >
                <Text style={[styles.segmentText, { color: selected ? '#fff' : colors.text }]}>{it.label()}</Text>
              </Pressable>
            );
          })}
        </View>
      </View>
      <Text style={[styles.heading, { color: colors.text }]}>{t('app.settings.openInApp')}</Text>
      <Text style={[styles.info, { color: colors.textMuted }]}>{t('app.settings.openInApp.info')}</Text>
      {NATIVE_PAGES.map((page) => (
        <View key={page} style={[styles.row, { borderColor: colors.border }]}>
          <Text style={[styles.label, { color: colors.text }]}>{pageLabel(page)}</Text>
          <Switch
            value={opensInApp(settings, page)}
            onValueChange={(inApp) => appSettings.setOpensInApp(page, inApp)}
          />
        </View>
      ))}
      <Pressable style={[styles.row, styles.logRow, { borderColor: colors.border }]} onPress={onShowLog}>
        <Text style={[styles.label, { color: colors.text }]}>{t('app.log.title')}</Text>
        <Text style={[styles.info, { color: colors.textMuted }]}>{t('app.log.settingsInfo')}</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 4 },
  heading: { fontSize: 17, fontWeight: '600', marginTop: 24 },
  info: { fontSize: 13, marginBottom: 8 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  logRow: { flexDirection: 'column', alignItems: 'flex-start', marginTop: 24, gap: 4 },
  label: { fontSize: 16 },
  fill: { flex: 1, marginRight: 12 },
  rowInfo: { marginBottom: 0, marginTop: 2 },
  segments: { flexDirection: 'row', borderWidth: StyleSheet.hairlineWidth, borderRadius: 8, overflow: 'hidden' },
  segment: { paddingHorizontal: 12, paddingVertical: 6 },
  segmentText: { fontSize: 14 },
});
