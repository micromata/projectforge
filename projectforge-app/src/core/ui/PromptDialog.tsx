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

import { useState } from 'react';
import { KeyboardAvoidingView, Modal, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { t } from '../i18n/translations';
import { useColors } from './colors';

/** Asks for one line of text (a name): [onDone] with the trimmed text, never with an empty one. */
export function PromptDialog({
  visible,
  title,
  label,
  initial,
  onDone,
  onCancel,
}: {
  visible: boolean;
  title: string;
  label: string;
  initial: string;
  onDone: (text: string) => void;
  onCancel: () => void;
}) {
  const colors = useColors();
  const [text, setText] = useState<string | null>(null);
  const value = text ?? initial;
  const close = (done: boolean) => {
    const trimmed = value.trim();
    setText(null);
    if (done && trimmed) onDone(trimmed);
    else onCancel();
  };
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={() => close(false)}>
      <KeyboardAvoidingView style={styles.backdrop} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={[styles.dialog, { backgroundColor: colors.background }]}>
          <Text style={[styles.title, { color: colors.text }]}>{title}</Text>
          <TextInput
            style={[styles.input, { color: colors.text, borderColor: colors.border }]}
            value={value}
            onChangeText={setText}
            placeholder={label}
            placeholderTextColor={colors.textMuted}
            autoFocus
            selectTextOnFocus
            returnKeyType="done"
            onSubmitEditing={() => close(true)}
          />
          <View style={styles.buttons}>
            <Pressable hitSlop={8} onPress={() => close(false)}>
              <Text style={[styles.button, { color: colors.textMuted }]}>{t('cancel')}</Text>
            </Pressable>
            <Pressable hitSlop={8} onPress={() => close(true)}>
              <Text style={[styles.button, { color: colors.primary }]}>{t('save')}</Text>
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'center', padding: 32, backgroundColor: 'rgba(0,0,0,0.35)' },
  dialog: { borderRadius: 12, padding: 20, gap: 16 },
  title: { fontSize: 17, fontWeight: '600' },
  input: { borderWidth: StyleSheet.hairlineWidth, borderRadius: 6, padding: 10, fontSize: 16 },
  buttons: { flexDirection: 'row', justifyContent: 'flex-end', gap: 24 },
  button: { fontSize: 16, fontWeight: '600' },
});
