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

import { Pressable, StyleSheet, Text, View } from 'react-native';

import { t } from '../i18n/translations';
import { useColors } from './colors';

/** An error with a retry, in place of the content that failed. */
export function ErrorView({ message, onRetry, retryLabel }: { message: string; onRetry?: () => void; retryLabel?: string }) {
  const colors = useColors();
  return (
    <View style={[styles.center, { backgroundColor: colors.background }]}>
      <Text style={[styles.message, { color: colors.text }]}>{message}</Text>
      {onRetry && (
        <Pressable style={[styles.button, { backgroundColor: colors.primary }]} onPress={onRetry}>
          <Text style={[styles.buttonText, { color: colors.onPrimary }]}>{retryLabel ?? t('refresh')}</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  message: { fontSize: 16, textAlign: 'center', marginBottom: 16 },
  button: { borderRadius: 6, paddingHorizontal: 20, paddingVertical: 10 },
  buttonText: { fontSize: 16 },
});
