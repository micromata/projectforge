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
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { authController } from '../../core/auth/authController';
import { errorText, i18n, t } from '../../core/i18n/translations';
import { userStatus, useUserStatus } from '../../core/status/userStatus';
import { severityColor, useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { webPathOfLink } from '../../core/web/links';
import {
  AppNotification,
  canAcknowledge,
  canDone,
  createdAt,
  htmlToText,
  NotificationsRepository,
  sortForDisplay,
} from './notifications';

type Loaded<T> = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'data'; value: T };

const repository = () => (authController.api ? new NotificationsRepository(authController.api) : null);

/**
 * The visible notifications (banners first, they stay until done) and the archived ones of the last 90 days. The list
 * is loaded again whenever the summary of the user status changes (count or newest id), so a poll or a push that
 * finds something new shows it. Port of the Flutter app's NotificationsScreen.
 *
 * @param onOpenLink Shows the page of a link (a path of the server); the list is closed by it.
 */
export function NotificationsScreen({ onOpenLink }: { onOpenLink: (path: string) => void }) {
  const colors = useColors();
  const summary = useUserStatus().status?.notifications;
  const summaryKey = `${summary?.openCount}/${summary?.latestId}`;
  const [open, setOpen] = useState<Loaded<AppNotification[]>>({ status: 'loading' });
  const [archived, setArchived] = useState<Loaded<AppNotification[]> | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  /** Loads the list; a failure keeps a shown list and is returned for the caller to report. */
  const load = useCallback(async (): Promise<unknown> => {
    const repo = repository();
    if (repo == null) return null;
    try {
      const list = await repo.loadOpen();
      setOpen({ status: 'data', value: list });
      return null;
    } catch (e) {
      setOpen((shown) => (shown.status === 'data' ? shown : { status: 'error', error: e }));
      return e;
    }
  }, []);

  const loadArchived = useCallback(async () => {
    const repo = repository();
    if (repo == null) return;
    setArchived({ status: 'loading' });
    try {
      setArchived({ status: 'data', value: await repo.loadArchived() });
    } catch (e) {
      setArchived({ status: 'error', error: e });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load, summaryKey]);

  const refresh = async () => {
    setRefreshing(true);
    const error = await load();
    await userStatus.refresh();
    setRefreshing(false);
    if (error != null) Alert.alert(errorText(error));
  };

  /** An action on a notification, then the list, the archive (if shown) and the status anew. */
  const act = async (action: (repo: NotificationsRepository) => Promise<void>): Promise<boolean> => {
    const repo = repository();
    if (repo == null) return false;
    try {
      await action(repo);
    } catch (e) {
      Alert.alert(errorText(e));
      return false;
    }
    if (archived != null) void loadArchived();
    await load();
    void userStatus.refresh();
    return true;
  };

  if (open.status === 'loading') return <ActivityIndicator style={styles.fill} />;
  if (open.status === 'error') return <ErrorView message={errorText(open.error)} onRetry={() => void load()} />;
  const list = sortForDisplay(open.value);
  return (
    <ScrollView
      style={{ backgroundColor: colors.background }}
      contentContainerStyle={styles.list}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void refresh()} />}
    >
      {list.length === 0 && <Text style={[styles.empty, { color: colors.text }]}>{t('notification.centerEmpty')}</Text>}
      {list.map((it) => (
        <NotificationCard
          key={it.id}
          notification={it}
          onOpen={(path) => {
            // Reading it is following the link: the notification is not left open behind.
            if (canAcknowledge(it)) void act((repo) => repo.acknowledge(it.id));
            onOpenLink(path);
          }}
          onAcknowledge={() => void act((repo) => repo.acknowledge(it.id))}
          onDone={() => void act((repo) => repo.done(it.id))}
        />
      ))}
      <Pressable
        style={styles.archivedHeader}
        onPress={() => (archived == null ? void loadArchived() : setArchived(null))}
      >
        <Text style={[styles.fill, { color: colors.text, fontSize: 16 }]}>{t('notification.centerArchived')}</Text>
        <Ionicons name={archived == null ? 'chevron-down' : 'chevron-up'} size={20} color={colors.textMuted} />
      </Pressable>
      {archived?.status === 'loading' && <ActivityIndicator style={styles.archivedLoading} />}
      {archived?.status === 'error' && (
        <ErrorView message={errorText(archived.error)} onRetry={() => void loadArchived()} />
      )}
      {archived?.status === 'data' && archived.value.length === 0 && (
        <Text style={[styles.archivedEmpty, { color: colors.textMuted }]}>{t('notification.centerArchivedEmpty')}</Text>
      )}
      {archived?.status === 'data' && archived.value.map((it) => <NotificationCard key={it.id} notification={it} archived />)}
    </ScrollView>
  );
}

function NotificationCard({
  notification,
  archived = false,
  onOpen,
  onAcknowledge,
  onDone,
}: {
  notification: AppNotification;
  archived?: boolean;
  onOpen?: (path: string) => void;
  onAcknowledge?: () => void;
  onDone?: () => void;
}) {
  const colors = useColors();
  const body = htmlToText(notification.body);
  const path = archived ? null : webPathOfLink(notification.link);
  const created = createdAt(notification);
  const unread = !archived && canAcknowledge(notification);
  return (
    <View
      style={[
        styles.card,
        { backgroundColor: colors.surface, borderLeftColor: severityColor(colors, notification.severity) ?? 'transparent' },
      ]}
    >
      <Text style={[styles.title, { color: colors.text }, unread && styles.bold]}>{notification.title ?? ''}</Text>
      {body ? <Text style={{ color: colors.text }}>{body}</Text> : null}
      {created && (
        <Text style={[styles.date, { color: colors.textMuted }]}>
          {created.toLocaleString(i18n.locale, { dateStyle: 'short', timeStyle: 'short' })}
        </Text>
      )}
      {!archived && (
        <View style={styles.actions}>
          {path && <Action label={t('notification.open')} onPress={() => onOpen?.(path)} />}
          {canAcknowledge(notification) && <Action label={t('notification.acknowledge')} onPress={onAcknowledge} />}
          {canDone(notification) && <Action label={t('notification.markDone')} onPress={onDone} />}
        </View>
      )}
    </View>
  );
}

function Action({ label, onPress }: { label: string; onPress?: () => void }) {
  const colors = useColors();
  return (
    <Pressable style={styles.action} onPress={onPress} hitSlop={4}>
      <Text style={{ color: colors.primary, fontWeight: '500' }}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  list: { padding: 12, gap: 8, paddingBottom: 24 },
  empty: { paddingVertical: 24, textAlign: 'center' },
  card: { borderRadius: 8, borderLeftWidth: 4, paddingTop: 10, paddingHorizontal: 12, paddingBottom: 4, gap: 4 },
  title: { fontSize: 15 },
  bold: { fontWeight: '700' },
  date: { fontSize: 12 },
  actions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 4 },
  action: { paddingHorizontal: 10, paddingVertical: 8 },
  archivedHeader: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, paddingHorizontal: 4 },
  archivedLoading: { padding: 16 },
  archivedEmpty: { padding: 16 },
});
