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
import {
  addMinutes,
  ceil5,
  formatDuration,
  formatTime,
  minutesBetween,
  sameDay,
} from '@projectforge/shared/booking-time';
import { ComponentProps, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ApiException } from '../../core/api/apiException';
import { errorText, t } from '../../core/i18n/translations';
import { useColors } from '../../core/ui/colors';
import { MenuItem, MenuSheet } from '../../core/ui/MenuSheet';
import { PromptDialog } from '../../core/ui/PromptDialog';
import { snackbar } from '../../core/ui/snackbar';
import { DateTimeRequest, DateTimeSheet, pickDateTime } from './DateTimeSheet';
import { dayShort } from './format';
import { TaskLabel } from './TaskLabel';
import { TemplateSheet } from './TemplateSheet';
import { BookingDraft, BookingWhat, Kost2Ref, kost2Number, kost2Text, shortPath, TaskRef, whatKey } from './timesheet';
import { BookingStart, timesheets, useTimesheets } from './timesheetsStore';

type IconName = ComponentProps<typeof Ionicons>['name'];
type Errors = Partial<Record<'task' | 'kost2' | 'time' | 'general', string>>;

/** The refusals of the server by the field they are about; whatever isn't about the task or cost unit is the period. */
function errorsOf(e: unknown): Errors {
  if (!(e instanceof ApiException) || e.validationErrors.length === 0) return { general: errorText(e) };
  const errors: Errors = {};
  for (const it of e.validationErrors) {
    const field = it.fieldId === 'task' || it.fieldId === 'kost2' ? it.fieldId : 'time';
    errors[field] = [errors[field], it.message].filter(Boolean).join('\n');
  }
  return errors;
}

/**
 * The booking form: *what* first (recent entries, templates, task, cost unit), then *when* in one row, then the text.
 * A screen of its own on a phone, a side panel on a tablet. Port of the Flutter app's BookingForm.
 *
 * @param tags The tags the installation offers, empty where none is configured (the form then has no tag).
 * @param onDone Called with the saved booking, or null on cancel.
 * @param onPickTask Shows the task picker; resolves with the chosen task, null if none.
 */
export function BookingForm({
  initial,
  tags,
  onDone,
  onDelete,
  onPickTask,
}: {
  initial: BookingStart;
  tags: string[];
  onDone: (saved: BookingDraft | null) => void;
  onDelete?: () => void;
  onPickTask: (recent: TaskRef[], current: TaskRef | null) => Promise<TaskRef | null>;
}) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { recent } = useTimesheets();
  const isEdit = initial.id != null;
  const what0 = initial.what;
  const [task, setTaskState] = useState<TaskRef | null>(what0?.task ?? null);
  const [kost2, setKost2] = useState<Kost2Ref | null>(what0?.task ? (what0.kost2 ?? null) : null);
  /** The cost units the task offers, null while they are loaded. */
  const [kost2Options, setKost2Options] = useState<Kost2Ref[] | null>(null);
  const [start, setStart] = useState(initial.start);
  const [stop, setStop] = useState(initial.stop);
  const [description, setDescription] = useState(what0?.description ?? '');
  const [location, setLocation] = useState(what0?.location ?? '');
  const [reference, setReference] = useState(what0?.reference ?? '');
  const [tag, setTag] = useState<string | null>(what0?.tag ?? null);
  const [errors, setErrors] = useState<Errors>({});
  const [saving, setSaving] = useState(false);
  const [more, setMore] = useState(false);
  const [menu, setMenu] = useState<'form' | 'kost2' | null>(null);
  const [templatesOpen, setTemplatesOpen] = useState(false);
  const [naming, setNaming] = useState(false);
  const [dateTime, setDateTime] = useState<DateTimeRequest | null>(null);
  const taskRef = useRef<TaskRef | null>(what0?.task ?? null);

  const repository = () => timesheets.repository();

  const clearErrors = (...fields: (keyof Errors)[]) =>
    setErrors((it) => {
      const next = { ...it };
      fields.forEach((field) => delete next[field]);
      return next;
    });

  /** Loads the cost units of [value]; [preset] stays if the task offers it, the only one is chosen. */
  const loadKost2 = (value: TaskRef, preset: Kost2Ref | null) => {
    repository()
      ?.taskInfo(value.id)
      .then(
        (info) => {
          if (taskRef.current?.id !== value.id) return;
          setKost2Options(info.kost2List);
          if (info.kost2List.length === 1) setKost2(info.kost2List[0]);
          else if (preset && !info.kost2List.some((it) => it.id === preset.id)) setKost2(null);
        },
        // The server reports a missing cost unit when saving.
        () => taskRef.current?.id === value.id && setKost2Options([]),
      );
  };

  const setTask = (value: TaskRef, preset: Kost2Ref | null = null) => {
    taskRef.current = value;
    setTaskState(value);
    setKost2(preset);
    setKost2Options(null);
    clearErrors('task', 'kost2');
    loadKost2(value, preset);
  };

  /** Fills the *what*, never the times (as the recent entries and templates of next do). */
  const apply = (what: BookingWhat) => {
    setDescription(what.description);
    setLocation(what.location);
    setReference(what.reference);
    setTag(what.tag);
    setErrors({});
    if (what.task == null) {
      taskRef.current = null;
      setTaskState(null);
      setKost2(null);
      setKost2Options(null);
    } else setTask(what.task, what.kost2);
  };

  useEffect(() => {
    timesheets.loadRecent();
    if (what0?.task) loadKost2(what0.task, what0.kost2);
    // Once for the form; a new start mounts a new form.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const duration = minutesBetween(start, stop);
  const nextDay = !sameDay(start, stop);
  const now = ceil5(new Date());
  const untilNow = sameDay(start, now) && now > start;

  const changeStart = (value: Date) => {
    setStart(value);
    setStop(addMinutes(value, duration));
    clearErrors('time');
  };

  /** A stop at or before the start counts as the next day (as next's time sheet form does). */
  const changeStopTime = (hours: number, minutes: number) => {
    let value = new Date(start.getFullYear(), start.getMonth(), start.getDate(), hours, minutes);
    if (value <= start) value = new Date(value.getFullYear(), value.getMonth(), value.getDate() + 1, hours, minutes);
    setStop(value);
    clearErrors('time');
  };

  const what = (): BookingWhat => ({
    task,
    kost2,
    description: description.trim(),
    location: location.trim(),
    reference: reference.trim(),
    tag,
  });

  const save = async () => {
    const repo = repository();
    if (task == null) {
      setErrors({ task: t('app.timesheet.task.required') });
      return;
    }
    if (repo == null) return;
    const draft: BookingDraft = { ...what(), task, start, stop };
    setSaving(true);
    setErrors({});
    try {
      await repo.save(draft, initial.id);
      timesheets.reloadWeeks();
      onDone(draft);
    } catch (e) {
      setSaving(false);
      setErrors(errorsOf(e));
    }
  };

  const saveAsTemplate = async (name: string) => {
    const repo = repository();
    if (repo == null || task == null) return;
    try {
      await repo.createTemplate(name, { ...what(), task });
      snackbar.show(t('app.timesheet.template.saved', name));
    } catch (e) {
      setErrors({ general: errorText(e) });
    }
  };

  const askTemplateName = () => {
    if (task == null) {
      setErrors({ task: t('app.timesheet.task.required') });
      return;
    }
    setNaming(true);
  };

  const pickTask = async () => {
    const fromBookings = recent.map((it) => it.task).filter((it): it is TaskRef => it != null);
    const picked = await onPickTask(fromBookings, task);
    if (picked) setTask(picked);
  };

  const pickTime = (isStart: boolean) =>
    pickDateTime(
      {
        mode: 'time',
        title: isStart ? t('app.timesheet.from') : t('app.timesheet.to'),
        value: isStart ? start : stop,
        onDone: (value) =>
          isStart
            ? changeStart(new Date(start.getFullYear(), start.getMonth(), start.getDate(), value.getHours(), value.getMinutes()))
            : changeStopTime(value.getHours(), value.getMinutes()),
      },
      setDateTime,
    );

  const pickDate = () =>
    pickDateTime(
      {
        mode: 'date',
        title: t('date'),
        value: start,
        onDone: (value) =>
          changeStart(new Date(value.getFullYear(), value.getMonth(), value.getDate(), start.getHours(), start.getMinutes())),
      },
      setDateTime,
    );

  const formItems: MenuItem[] = [
    { key: 'template', label: t('app.timesheet.template.save'), icon: 'bookmark-outline', onPress: askTemplateName },
    ...(isEdit && onDelete
      ? [{ key: 'delete', label: t('delete'), icon: 'trash-outline' as IconName, destructive: true, onPress: onDelete }]
      : []),
  ];
  const kost2Items: MenuItem[] = (kost2Options ?? []).map((it) => ({
    key: String(it.id),
    label: kost2Text(it) ? `${kost2Number(it)}  ${kost2Text(it)}` : kost2Number(it),
    icon: it.id === kost2?.id ? 'checkmark' : undefined,
    onPress: () => {
      setKost2(it);
      clearErrors('kost2');
    },
  }));
  const allTags = [...tags, ...(tag && !tags.includes(tag) ? [tag] : [])];
  const locations = [...new Set(recent.map((it) => it.location).filter((it) => it.length > 0))];
  const moreSummary = [location, reference, tag].filter((it): it is string => !!it).join(' · ');
  const field = { color: colors.text, borderColor: colors.border };

  return (
    <KeyboardAvoidingView style={[styles.fill, { backgroundColor: colors.background }]} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={[styles.header, { borderColor: colors.border }]}>
        <Pressable accessibilityLabel={t('cancel')} hitSlop={8} onPress={() => onDone(null)}>
          <Ionicons name="close" size={24} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]}>
          {isEdit ? t('timesheet.title.edit') : t('timesheet.title.add')}
        </Text>
        <Pressable accessibilityLabel={t('app.menu.more')} hitSlop={8} onPress={() => setMenu('form')}>
          <Ionicons name="ellipsis-vertical" size={22} color={colors.text} />
        </Pressable>
      </View>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.labelRow}>
          <Text style={[styles.label, { color: colors.text }]}>{t('app.timesheet.what')}</Text>
          <Pressable style={styles.link} onPress={() => setTemplatesOpen(true)}>
            <Ionicons name="bookmark-outline" size={16} color={colors.primary} />
            <Text style={{ color: colors.primary }}>{t('timesheet.templates')}</Text>
          </Pressable>
        </View>
        {!isEdit && recent.length > 0 && (
          <>
            <Text style={[styles.subLabel, { color: colors.textMuted }]}>{t('timesheet.recent')}</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.recent}>
              {recent.map((it) => {
                const selected = it.task?.id === task?.id && it.kost2?.id === kost2?.id && it.description === description;
                return (
                  <Pressable
                    key={whatKey(it)}
                    style={[styles.recentCard, { backgroundColor: selected ? colors.border : colors.surface }]}
                    onPress={() => apply(it)}
                  >
                    <Text style={[styles.recentTask, { color: colors.text }]} numberOfLines={1}>
                      {it.task ? shortPath(it.task) : ''}
                    </Text>
                    <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
                      {it.description}
                    </Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          </>
        )}
        <Pressable
          style={[styles.taskCard, { borderColor: errors.task ? colors.danger : colors.border }]}
          onPress={() => void pickTask()}
        >
          <Ionicons name={task ? 'folder-outline' : 'search'} size={20} color={colors.textMuted} />
          <View style={styles.fill}>
            {task ? <TaskLabel task={task} /> : <Text style={{ color: colors.text }}>{t('app.timesheet.task.choose')}</Text>}
          </View>
          <Ionicons name="chevron-forward" size={20} color={colors.textMuted} />
        </Pressable>
        <ErrorText text={errors.task} />
        {task && kost2Options == null && <ActivityIndicator style={styles.kost2Loading} />}
        {task && kost2Options?.length === 1 && (
          <Text style={[styles.kost2Single, { color: colors.textMuted }]}>
            {t('fibu.kost2')}: {kost2Options[0].label}
          </Text>
        )}
        {task && kost2Options != null && kost2Options.length > 1 && (
          <Pressable
            style={[styles.select, { borderColor: errors.kost2 ? colors.danger : colors.border }]}
            onPress={() => setMenu('kost2')}
          >
            <View style={styles.fill}>
              <Text style={{ color: colors.textMuted, fontSize: 12 }}>{t('fibu.kost2')}</Text>
              <Text style={{ color: colors.text }} numberOfLines={1}>
                {kost2?.label ?? '–'}
              </Text>
            </View>
            <Ionicons name="chevron-down" size={18} color={colors.textMuted} />
          </Pressable>
        )}
        <ErrorText text={errors.kost2} />

        <View style={styles.labelRow}>
          <Text style={[styles.label, { color: colors.text }]}>{t('app.timesheet.when')}</Text>
          <Pressable style={styles.link} onPress={pickDate}>
            <Ionicons name="calendar-outline" size={16} color={colors.primary} />
            <Text style={{ color: colors.primary }}>{dayShort(start)}</Text>
          </Pressable>
        </View>
        <View style={styles.whenRow}>
          <TimeButton label={t('app.timesheet.from')} time={start} error={!!errors.time} onPress={() => pickTime(true)} />
          <Ionicons name="arrow-forward" size={20} color={colors.textMuted} />
          <TimeButton
            label={nextDay ? t('app.timesheet.toNextDay') : t('app.timesheet.to')}
            time={stop}
            error={!!errors.time}
            onPress={() => pickTime(false)}
          />
          <Text style={[styles.duration, { color: colors.primary }]} numberOfLines={1} adjustsFontSizeToFit>
            {formatDuration(duration)} h
          </Text>
        </View>
        <ErrorText text={errors.time} />
        <View style={styles.chips}>
          {[15, 30, 60].map((minutes) => (
            <Chip
              key={minutes}
              label={minutes === 60 ? '+1 h' : `+${minutes} min`}
              onPress={() => {
                setStop(addMinutes(stop, minutes));
                clearErrors('time');
              }}
            />
          ))}
          {untilNow && (
            <Chip icon="time-outline" label={t('app.timesheet.untilNow')} onPress={() => changeStopTime(now.getHours(), now.getMinutes())} />
          )}
        </View>

        <Text style={[styles.label, styles.labelAlone, { color: colors.text }]}>{t('app.timesheet.whatDone')}</Text>
        <TextInput
          style={[styles.input, styles.multiline, field]}
          value={description}
          onChangeText={setDescription}
          placeholder={t('timesheet.description')}
          placeholderTextColor={colors.textMuted}
          multiline
          autoCapitalize="sentences"
        />
        <Pressable style={styles.moreHeader} onPress={() => setMore(!more)}>
          <View style={styles.fill}>
            <Text style={{ color: colors.text, fontSize: 16 }}>{t('app.timesheet.more')}</Text>
            {moreSummary ? <Text style={{ color: colors.textMuted }}>{moreSummary}</Text> : null}
          </View>
          <Ionicons name={more ? 'chevron-up' : 'chevron-down'} size={20} color={colors.textMuted} />
        </Pressable>
        {more && (
          <View style={styles.moreBody}>
            <TextInput
              style={[styles.input, field]}
              value={location}
              onChangeText={setLocation}
              placeholder={t('timesheet.location')}
              placeholderTextColor={colors.textMuted}
            />
            {locations.length > 0 && (
              <View style={styles.chips}>
                {locations.map((it) => (
                  <Chip key={it} label={it} onPress={() => setLocation(it)} />
                ))}
              </View>
            )}
            <TextInput
              style={[styles.input, field]}
              value={reference}
              onChangeText={setReference}
              placeholder={t('timesheet.reference')}
              placeholderTextColor={colors.textMuted}
            />
            {allTags.length > 0 && (
              <View style={styles.chips}>
                {allTags.map((it) => (
                  <Chip key={it} label={it} selected={tag === it} onPress={() => setTag(tag === it ? null : it)} />
                ))}
              </View>
            )}
          </View>
        )}
      </ScrollView>
      <View style={[styles.saveBar, { borderColor: colors.border, paddingBottom: insets.bottom + 8 }]}>
        {errors.general && (
          <View style={styles.general}>
            <Ionicons name="cloud-offline-outline" size={18} color={colors.danger} />
            <Text style={[styles.fill, { color: colors.text }]}>{errors.general}</Text>
          </View>
        )}
        <Pressable
          style={[styles.save, { backgroundColor: colors.primary, opacity: saving ? 0.6 : 1 }]}
          disabled={saving}
          onPress={() => void save()}
        >
          {saving ? (
            <ActivityIndicator color={colors.onPrimary} />
          ) : (
            <Text style={[styles.saveText, { color: colors.onPrimary }]}>
              {isEdit ? t('save') : t('app.timesheet.bookWithDuration', formatDuration(duration))}
            </Text>
          )}
        </Pressable>
      </View>
      <MenuSheet
        visible={menu != null}
        items={menu === 'form' ? formItems : menu === 'kost2' ? kost2Items : []}
        onClose={() => setMenu(null)}
      />
      <TemplateSheet
        visible={templatesOpen}
        onClose={() => setTemplatesOpen(false)}
        onApply={async (template) => {
          try {
            const repo = repository();
            if (repo) apply(await repo.whatOf(template));
          } catch (e) {
            setErrors({ general: errorText(e) });
          }
        }}
        onSaveCurrent={askTemplateName}
      />
      <PromptDialog
        visible={naming}
        title={t('app.timesheet.template.save')}
        label={t('name')}
        initial={task?.title ?? ''}
        onDone={(name) => {
          setNaming(false);
          void saveAsTemplate(name);
        }}
        onCancel={() => setNaming(false)}
      />
      <DateTimeSheet request={dateTime} onClose={() => setDateTime(null)} />
    </KeyboardAvoidingView>
  );
}

function ErrorText({ text }: { text?: string }) {
  const colors = useColors();
  if (!text) return null;
  return (
    <View style={styles.error}>
      <Ionicons name="alert-circle-outline" size={18} color={colors.danger} />
      <Text style={[styles.fill, { color: colors.danger }]}>{text}</Text>
    </View>
  );
}

function TimeButton({ label, time, error, onPress }: { label: string; time: Date; error: boolean; onPress: () => void }) {
  const colors = useColors();
  return (
    <Pressable style={[styles.timeButton, { borderColor: error ? colors.danger : colors.border }]} onPress={onPress}>
      <Text style={{ color: colors.textMuted, fontSize: 11 }}>{label}</Text>
      <Text style={{ color: colors.text, fontSize: 22, fontWeight: '500' }}>{formatTime(time)}</Text>
    </Pressable>
  );
}

function Chip({ label, icon, selected, onPress }: { label: string; icon?: IconName; selected?: boolean; onPress: () => void }) {
  const colors = useColors();
  return (
    <Pressable
      style={[styles.chip, { borderColor: colors.border, backgroundColor: selected ? colors.border : 'transparent' }]}
      accessibilityState={{ selected }}
      onPress={onPress}
    >
      {icon && <Ionicons name={icon} size={16} color={colors.text} />}
      <Text style={{ color: colors.text }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12, borderBottomWidth: StyleSheet.hairlineWidth },
  headerTitle: { flex: 1, fontSize: 20, fontWeight: '600' },
  content: { paddingHorizontal: 16, paddingBottom: 24 },
  labelRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 20, paddingBottom: 8 },
  label: { fontSize: 16, fontWeight: '600' },
  labelAlone: { paddingTop: 20, paddingBottom: 8 },
  subLabel: { fontSize: 12, paddingBottom: 6 },
  link: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  recent: { gap: 8, paddingBottom: 12 },
  recentCard: { width: 180, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 8 },
  recentTask: { fontSize: 14, fontWeight: '600' },
  taskCard: { flexDirection: 'row', alignItems: 'center', gap: 12, borderWidth: 1, borderRadius: 12, padding: 12 },
  kost2Loading: { paddingTop: 12 },
  kost2Single: { paddingTop: 8, paddingLeft: 16, fontSize: 12 },
  select: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderRadius: 6, padding: 10, marginTop: 12 },
  whenRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  timeButton: { borderWidth: 1, borderRadius: 8, paddingHorizontal: 14, paddingVertical: 6 },
  duration: { flex: 1, textAlign: 'right', fontSize: 24, fontWeight: '500' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, paddingTop: 8 },
  chip: { flexDirection: 'row', alignItems: 'center', gap: 4, borderWidth: 1, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 6 },
  input: { borderWidth: StyleSheet.hairlineWidth, borderRadius: 6, padding: 10, fontSize: 16, marginTop: 8 },
  multiline: { minHeight: 64, textAlignVertical: 'top' },
  moreHeader: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14 },
  moreBody: { paddingBottom: 8 },
  saveBar: { borderTopWidth: StyleSheet.hairlineWidth, paddingHorizontal: 16, paddingTop: 8 },
  general: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingBottom: 8 },
  save: { height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
  saveText: { fontSize: 16, fontWeight: '600' },
  error: { flexDirection: 'row', alignItems: 'flex-start', gap: 6, paddingTop: 6 },
});
