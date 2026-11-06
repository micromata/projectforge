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
import { ComponentProps, ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useColors } from './colors';

/** The bar of a native tab: its title and a few icon actions (the pages of the server bring their own header). */
export function ScreenHeader({ title, children }: { title: string; children?: ReactNode }) {
  const colors = useColors();
  return (
    <View style={[styles.bar, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      <Text style={[styles.title, { color: colors.text }]} numberOfLines={1} accessibilityRole="header">
        {title}
      </Text>
      {children}
    </View>
  );
}

/** An icon action of a [ScreenHeader] (the label is its accessibility label). */
export function HeaderAction({
  icon,
  label,
  onPress,
  disabled,
  color,
}: {
  icon: ComponentProps<typeof Ionicons>['name'];
  label: string;
  onPress: () => void;
  disabled?: boolean;
  color?: string;
}) {
  const colors = useColors();
  return (
    <Pressable style={styles.action} accessibilityLabel={label} hitSlop={8} disabled={disabled} onPress={onPress}>
      <Ionicons name={icon} size={22} color={disabled ? colors.border : (color ?? colors.text)} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 48,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    gap: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  title: { flex: 1, fontSize: 18, fontWeight: '600' },
  action: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
});
