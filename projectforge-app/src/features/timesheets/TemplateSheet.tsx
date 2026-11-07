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
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { fullPath, TimesheetTemplate } from './timesheet';
import { timesheets } from './timesheetsStore';
import { MODAL_ORIENTATIONS } from '../../core/ui/modalOrientations';

type Loaded = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'data'; list: TimesheetTemplate[] };

/** The templates of the user: a tap applies one, the bin deletes it, the last row saves the current entry. */
export function TemplateSheet({
  visible,
  onClose,
  onApply,
  onSaveCurrent,
}: {
  visible: boolean;
  onClose: () => void;
  onApply: (template: TimesheetTemplate) => void;
  onSaveCurrent: () => void;
}) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const [templates, setTemplates] = useState<Loaded>({ status: 'loading' });
  /** The paths of the templates' tasks, loaded on demand. */
  const [paths, setPaths] = useState<Record<number, string>>({});
  /** Runs once the sheet is gone (iOS can't present a dialog over a modal that is still being dismissed). */
  const pending = useRef<(() => void) | null>(null);
  const closeThen = (action: () => void) => {
    pending.current = action;
    onClose();
    if (Platform.OS !== 'ios') runPending();
  };
  const runPending = () => {
    const action = pending.current;
    pending.current = null;
    action?.();
  };

  const load = () => {
    const repo = timesheets.repository();
    if (repo == null) return;
    repo.templates().then(
      (list) => {
        setTemplates({ status: 'data', list });
        for (const taskId of new Set(list.map((it) => it.taskId).filter((it): it is number => it != null))) {
          repo
            .taskInfo(taskId)
            .then((info) => setPaths((it) => ({ ...it, [taskId]: fullPath(info.task) })))
            .catch(() => undefined);
        }
      },
      (error: unknown) => setTemplates({ status: 'error', error }),
    );
  };

  useEffect(() => {
    if (visible) load();
  }, [visible]);

  const remove = (template: TimesheetTemplate) =>
    Alert.alert('', t('app.timesheet.template.delete.question', template.name), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('delete'),
        style: 'destructive',
        onPress: () => {
          timesheets
            .repository()
            ?.deleteTemplate(template.id)
            .then(load, (e: unknown) => Alert.alert(errorText(e)));
        },
      },
    ]);

  return (
    <Modal
      supportedOrientations={MODAL_ORIENTATIONS}
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
      onDismiss={runPending}
    >
      <Pressable style={styles.backdrop} onPress={onClose}>
        <View
          style={[styles.sheet, { backgroundColor: colors.surface, paddingBottom: insets.bottom + 8 }]}
          onStartShouldSetResponder={() => true}
        >
          <Text style={[styles.title, { color: colors.text }]}>{t('timesheet.templates')}</Text>
          <ScrollView>
            {templates.status === 'loading' && <ActivityIndicator style={styles.padded} />}
            {templates.status === 'error' && (
              <Text style={[styles.padded, { color: colors.text }]}>{errorText(templates.error)}</Text>
            )}
            {templates.status === 'data' && templates.list.length === 0 && (
              <Text style={[styles.padded, { color: colors.text }]}>{t('app.timesheet.template.none')}</Text>
            )}
            {templates.status === 'data' &&
              templates.list.map((it) => {
                const lines = [it.taskId != null ? paths[it.taskId] : undefined, it.description].filter(Boolean).join('\n');
                return (
                  <Pressable
                    key={it.id}
                    style={styles.row}
                    onPress={() => closeThen(() => onApply(it))}
                  >
                    <Ionicons name="bookmark-outline" size={20} color={colors.textMuted} />
                    <View style={styles.fill}>
                      <Text style={{ color: colors.text, fontWeight: '600' }}>{it.name}</Text>
                      {lines ? (
                        <Text style={{ color: colors.textMuted }} numberOfLines={3}>
                          {lines}
                        </Text>
                      ) : null}
                    </View>
                    <Pressable accessibilityLabel={t('delete')} hitSlop={8} onPress={() => remove(it)}>
                      <Ionicons name="trash-outline" size={20} color={colors.textMuted} />
                    </Pressable>
                  </Pressable>
                );
              })}
            <Pressable
              style={[styles.row, { borderTopWidth: StyleSheet.hairlineWidth, borderColor: colors.border }]}
              onPress={() => closeThen(onSaveCurrent)}
            >
              <Ionicons name="bookmark" size={20} color={colors.primary} />
              <Text style={{ color: colors.primary }}>{t('app.timesheet.template.save')}</Text>
            </Pressable>
          </ScrollView>
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: { maxHeight: '70%', borderTopLeftRadius: 12, borderTopRightRadius: 12, paddingTop: 12 },
  title: { fontSize: 17, fontWeight: '600', paddingHorizontal: 16, paddingBottom: 8 },
  padded: { padding: 16 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 12 },
});
