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
import { ComponentProps } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useColors } from './colors';

export interface MenuItem {
  key: string;
  label: string;
  icon?: ComponentProps<typeof Ionicons>['name'];
  disabled?: boolean;
  destructive?: boolean;
  /** A text only (e.g. the version), not a choice. */
  info?: boolean;
  onPress?: () => void;
}

/** A menu sliding up from the bottom (the browser functions, the account menu). */
export function MenuSheet({ visible, items, onClose }: { visible: boolean; items: MenuItem[]; onClose: () => void }) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <View
          style={[styles.sheet, { backgroundColor: colors.surface, paddingBottom: insets.bottom + 8 }]}
          onStartShouldSetResponder={() => true}
        >
          {items.map((item) => (
            <Pressable
              key={item.key}
              disabled={item.disabled || item.info}
              style={({ pressed }) => [styles.item, pressed && { backgroundColor: colors.border }]}
              onPress={() => {
                onClose();
                item.onPress?.();
              }}
            >
              {item.icon && (
                <Ionicons
                  name={item.icon}
                  size={20}
                  color={item.disabled ? colors.border : item.destructive ? colors.danger : colors.text}
                />
              )}
              <Text
                style={[
                  styles.label,
                  { color: item.disabled ? colors.border : item.destructive ? colors.danger : colors.text },
                  item.info && { color: colors.textMuted, fontSize: 14 },
                ]}
              >
                {item.label}
              </Text>
            </Pressable>
          ))}
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: { borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingTop: 8 },
  item: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingHorizontal: 20, paddingVertical: 13 },
  label: { fontSize: 16, flexShrink: 1 },
});
