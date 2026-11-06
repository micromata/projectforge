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
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { AppLoginCancelled } from '../../core/auth/appLogin';
import { authController } from '../../core/auth/authController';

/** The server url, then the login in the system browser. */
export function LoginScreen({ message, onShowLog }: { message?: string; onShowLog: () => void }) {
  const [server, setServer] = useState(process.env.EXPO_PUBLIC_PF_SERVER ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | undefined>(message);

  const login = async () => {
    setBusy(true);
    setError(undefined);
    try {
      await authController.login(server);
    } catch (e) {
      if (!(e instanceof AppLoginCancelled)) setError(String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>ProjectForge</Text>
      <Text style={styles.subtitle}>React Native spike</Text>
      <TextInput
        style={styles.input}
        value={server}
        onChangeText={setServer}
        placeholder="https://projectforge.example.org"
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType="url"
        onSubmitEditing={login}
      />
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <Pressable style={styles.button} onPress={login} disabled={busy}>
        {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Log in</Text>}
      </Pressable>
      <Pressable onPress={onShowLog}>
        <Text style={styles.link}>Log</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, gap: 12 },
  title: { fontSize: 28, fontWeight: '600', textAlign: 'center' },
  subtitle: { fontSize: 14, color: '#666', textAlign: 'center', marginBottom: 24 },
  input: { borderWidth: StyleSheet.hairlineWidth, borderColor: '#999', borderRadius: 6, padding: 12, fontSize: 16 },
  error: { color: '#c62828' },
  button: { backgroundColor: '#1976d2', borderRadius: 6, padding: 14, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16 },
  link: { color: '#1976d2', textAlign: 'center', marginTop: 12 },
});
