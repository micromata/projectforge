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
import { ComponentProps, useRef } from 'react';
import { Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useColors } from './colors';
import { MODAL_ORIENTATIONS } from './modalOrientations';

export interface MenuItem {
  key: string;
  label: string;
  icon?: ComponentProps<typeof Ionicons>['name'];
  disabled?: boolean;
  destructive?: boolean;
  /** A text only (e.g. the version), not a choice. */
  info?: boolean;
  /** A second line below the label, in small letters. */
  detail?: string;
  /** The current choice of a picker: marked with a check. */
  selected?: boolean;
  /** A count at the end, as the badges of the tab bar (none for 0). */
  badge?: number;
  onPress?: () => void;
}

/**
 * A menu sliding up from the bottom (the browser functions, the account menu, a picker with [title]), scrolled within
 * if it has more items than fit. On iOS the chosen action runs once the menu is gone: a share sheet, the Wallet or an
 * alert can't be presented over a modal that is still being dismissed.
 */
export function MenuSheet({
  visible,
  items,
  onClose,
  title,
}: {
  visible: boolean;
  items: MenuItem[];
  onClose: () => void;
  title?: string;
}) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const pending = useRef<(() => void) | null>(null);
  const runPending = () => {
    const action = pending.current;
    pending.current = null;
    action?.();
  };
  return (
    <Modal
      supportedOrientations={MODAL_ORIENTATIONS}
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
      onDismiss={runPending}
    >
      <Pressable style={styles.backdrop} onPress={onClose}>
        <View
          // Left and right: in landscape the sheet would reach under the notch (the Dynamic Island).
          style={[
            styles.sheet,
            {
              backgroundColor: colors.surface,
              paddingBottom: insets.bottom + 8,
              paddingLeft: insets.left,
              paddingRight: insets.right,
            },
          ]}
          onStartShouldSetResponder={() => true}
        >
          {title && <Text style={[styles.title, { color: colors.text, borderColor: colors.border }]}>{title}</Text>}
          <ScrollView bounces={false}>
            {items.map((item) => (
              <Pressable
                key={item.key}
                disabled={item.disabled || item.info}
                style={({ pressed }) => [styles.item, pressed && { backgroundColor: colors.border }]}
                onPress={() => {
                  pending.current = item.onPress ?? null;
                  onClose();
                  if (Platform.OS !== 'ios') runPending();
                }}
              >
                {item.icon && (
                  <Ionicons
                    name={item.icon}
                    size={20}
                    color={item.disabled ? colors.border : item.destructive ? colors.danger : colors.text}
                  />
                )}
                <View style={styles.texts}>
                  <Text
                    style={[
                      styles.label,
                      { color: item.disabled ? colors.border : item.destructive ? colors.danger : colors.text },
                      item.info && { color: colors.textMuted, fontSize: 14 },
                      item.selected && styles.selected,
                    ]}
                  >
                    {item.label}
                  </Text>
                  {item.detail ? (
                    <Text style={[styles.detail, { color: colors.textMuted }]} numberOfLines={2}>
                      {item.detail}
                    </Text>
                  ) : null}
                </View>
                {item.badge != null && item.badge > 0 && (
                  <View style={[styles.badge, { backgroundColor: colors.badge }]}>
                    <Text style={styles.badgeText}>{item.badge > 99 ? '99+' : item.badge}</Text>
                  </View>
                )}
                {item.selected && <Ionicons name="checkmark" size={20} color={colors.primary} />}
              </Pressable>
            ))}
          </ScrollView>
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: { maxHeight: '75%', borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingTop: 8 },
  title: { fontSize: 17, fontWeight: '600', paddingHorizontal: 20, paddingTop: 6, paddingBottom: 12, borderBottomWidth: StyleSheet.hairlineWidth },
  item: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingHorizontal: 20, paddingVertical: 13 },
  texts: { flex: 1 },
  label: { fontSize: 16 },
  selected: { fontWeight: '600' },
  detail: { fontSize: 13, marginTop: 2 },
  badge: {
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    paddingHorizontal: 5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#fff', fontSize: 12, fontWeight: '600' },
});
