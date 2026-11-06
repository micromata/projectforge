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

import { ApiException } from '../../core/api/apiException';
import { AppLoginCancelled } from '../../core/auth/appLogin';
import { authController, InsecureServerUrl, InvalidServerUrl } from '../../core/auth/authController';
import { Session } from '../../core/auth/session';
import { t } from '../../core/i18n/translations';
import { appLog } from '../../core/log/appLog';
import { useColors } from '../../core/ui/colors';

/** The development server of `bin/pfApp.sh run <slot>` (only set by it, inlined at bundle time). */
const DEV_SERVER_URL = process.env.EXPO_PUBLIC_PF_SERVER ?? '';

/**
 * The server url, then the login in the system browser. The servers of the last logins are offered; one with a stored
 * login is switched to at once, without the browser. Port of the Flutter app's LoginScreen.
 *
 * @param addServer Opened while logged in, to add another server: [onDone] closes it again.
 */
export function LoginScreen({
  messageKey,
  addServer = false,
  onDone,
  onShowLog,
}: {
  messageKey?: string;
  addServer?: boolean;
  onDone?: () => void;
  onShowLog: () => void;
}) {
  const colors = useColors();
  const [server, setServer] = useState('');
  const [servers, setServers] = useState<string[]>([]);
  const [sessions, setSessions] = useState<Record<string, Session>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | undefined>();

  const applyServers = useCallback((list: string[], stored: Session[]) => {
    setServers(list);
    setSessions(Object.fromEntries(stored.map((it) => [it.serverUrl, it])));
  }, []);

  const loadServers = async () =>
    applyServers(...(await Promise.all([authController.servers(), authController.sessions()])));

  useEffect(() => {
    let cancelled = false;
    void Promise.all([authController.servers(), authController.sessions()]).then(([list, stored]) => {
      if (cancelled) return;
      applyServers(list, stored);
      // The last server, otherwise the development server.
      const preset = list[0] ?? DEV_SERVER_URL;
      if (preset) setServer((current) => current || preset);
    });
    return () => {
      cancelled = true;
    };
  }, [applyServers]);

  const login = async () => {
    setBusy(true);
    setError(undefined);
    try {
      await authController.login(server);
      onDone?.();
    } catch (e) {
      if (e instanceof AppLoginCancelled) {
        // The user closed the browser: nothing to report.
      } else if (e instanceof InvalidServerUrl) {
        setError(t('app.server.invalid'));
      } else if (e instanceof InsecureServerUrl) {
        setError(t('app.server.httpsRequired'));
      } else if (e instanceof ApiException && e.statusCode != null) {
        setError(`${t('app.login.failed')} (${e.message})`);
      } else {
        appLog.warn('login', `Server not reached: ${String(e)}`);
        setError(t('app.server.unreachable'));
      }
    } finally {
      setBusy(false);
    }
  };

  /** A server with a stored login is switched to at once, any other one only put into the field. */
  const tap = async (serverUrl: string) => {
    const stored = sessions[serverUrl];
    if (stored == null) {
      setServer(serverUrl);
      setError(undefined);
      return;
    }
    await authController.switchTo(stored);
    onDone?.();
  };

  const remove = async (serverUrl: string) => {
    await authController.removeServer(serverUrl);
    await loadServers();
  };

  const devServer = DEV_SERVER_URL && !servers.includes(DEV_SERVER_URL) ? DEV_SERVER_URL : null;
  const offered = [...servers, ...(devServer ? [devServer] : [])];
  return (
    <KeyboardAvoidingView
      style={[styles.fill, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={[styles.title, { color: colors.text }]}>ProjectForge</Text>
        {/* Why the app is logged out (e.g. the session expired): stays while the user tries again. */}
        {messageKey ? <Text style={[styles.error, { color: colors.danger }]}>{t(messageKey)}</Text> : null}
        {error ? <Text style={[styles.error, { color: colors.danger }]}>{error}</Text> : null}
        <Text style={[styles.label, { color: colors.textMuted }]}>{t('app.server.label')}</Text>
        <TextInput
          style={[styles.input, { borderColor: colors.border, color: colors.text }]}
          value={server}
          onChangeText={setServer}
          placeholder="projectforge.example.org"
          placeholderTextColor={colors.textMuted}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          returnKeyType="go"
          onSubmitEditing={login}
          editable={!busy}
        />
        {offered.length > 0 && (
          <View style={styles.servers}>
            <Text style={[styles.label, { color: colors.textMuted }]}>{t('app.server.recent')}</Text>
            {offered.map((url) => {
              const signedIn = sessions[url] != null;
              const current = authController.getState().session?.serverUrl === url;
              return (
                <View key={url} style={[styles.serverRow, { borderColor: colors.border }]}>
                  <Pressable style={styles.serverTap} disabled={busy} onPress={() => void tap(url)}>
                    <Ionicons name="server-outline" size={20} color={colors.textMuted} />
                    <View style={styles.fill}>
                      <Text style={{ color: colors.text }} numberOfLines={1}>
                        {url}
                      </Text>
                      {signedIn && <Text style={{ color: colors.textMuted, fontSize: 12 }}>{t('app.server.loggedIn')}</Text>}
                    </View>
                  </Pressable>
                  {url !== devServer && !current && (
                    <Pressable
                      accessibilityLabel={t('app.server.remove')}
                      hitSlop={8}
                      onPress={() => void remove(url)}
                    >
                      <Ionicons name="close" size={20} color={colors.textMuted} />
                    </Pressable>
                  )}
                </View>
              );
            })}
          </View>
        )}
        <Text style={[styles.info, { color: colors.textMuted }]}>{t('app.login.info')}</Text>
        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }]}
          onPress={login}
          disabled={busy || !server.trim()}
        >
          {busy ? (
            <ActivityIndicator color={colors.onPrimary} />
          ) : (
            <Text style={[styles.buttonText, { color: colors.onPrimary }]}>{t('login')}</Text>
          )}
        </Pressable>
        <Pressable onPress={onShowLog} style={styles.logLink}>
          <Text style={{ color: colors.primary }}>{t('app.log.title')}</Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  container: { flexGrow: 1, justifyContent: 'center', padding: 24, gap: 12 },
  title: { fontSize: 28, fontWeight: '600', textAlign: 'center', marginBottom: 16 },
  label: { fontSize: 13 },
  input: { borderWidth: StyleSheet.hairlineWidth, borderRadius: 6, padding: 12, fontSize: 16 },
  servers: { gap: 4 },
  serverRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    paddingVertical: 8,
  },
  serverTap: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 12 },
  info: { fontSize: 13 },
  error: { fontSize: 14 },
  button: { borderRadius: 6, padding: 14, alignItems: 'center', marginTop: 4 },
  buttonText: { fontSize: 16 },
  logLink: { alignItems: 'center', paddingTop: 12 },
});
