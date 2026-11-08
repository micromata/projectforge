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
import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { GestureDetector } from 'react-native-gesture-handler';

import { t } from '../i18n/translations';
import { useColors } from './colors';
import { TabSheet } from './TabSheet';

/**
 * A [TabSheet] of the full height with a title on top, pulled down by it: the notifications, the settings, the server
 * switch. [onBack]: a page within the sheet is shown, the arrow beside the title (and the back button of Android) goes
 * back to the first one.
 */
export function TitledTabSheet({
  open,
  title,
  onClose,
  onBack,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  onBack?: () => void;
  children: ReactNode;
}) {
  const colors = useColors();
  return (
    <TabSheet open={open} closeLabel={t('close')} onClose={onClose} onBack={onBack}>
      {(pull) => (
        <View style={[styles.content, { backgroundColor: colors.background }]}>
          <GestureDetector gesture={pull}>
            <View style={[styles.top, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <View style={[styles.handle, { backgroundColor: colors.textMuted }]} />
              <Text style={[styles.title, { color: colors.text }]} accessibilityRole="header">
                {title}
              </Text>
              {onBack && (
                <Pressable
                  style={styles.back}
                  accessibilityRole="button"
                  accessibilityLabel={t('back')}
                  onPress={onBack}
                >
                  <Ionicons name="chevron-back" size={24} color={colors.primary} />
                </Pressable>
              )}
            </View>
          </GestureDetector>
          {children}
        </View>
      )}
    </TabSheet>
  );
}

const styles = StyleSheet.create({
  content: {
    flex: 1,
    borderTopLeftRadius: 14,
    borderTopRightRadius: 14,
    overflow: 'hidden',
  },
  top: {
    alignItems: 'center',
    paddingTop: 6,
    paddingBottom: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  handle: {
    width: 36,
    height: 5,
    borderRadius: 3,
    opacity: 0.5,
    marginBottom: 8,
  },
  title: { fontSize: 17, fontWeight: '600' },
  back: {
    position: 'absolute',
    left: 4,
    bottom: 0,
    height: 44,
    width: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
