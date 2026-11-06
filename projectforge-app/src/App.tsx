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

import { StatusBar } from 'expo-status-bar';
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import { authController, useAuth } from './core/auth/authController';
import { LogScreen } from './core/log/LogScreen';
import { LoginScreen } from './features/login/LoginScreen';
import { WebScreen, WebScreenHandle, WebScreenState } from './features/web/WebScreen';

type Tab = 'pages' | 'chat';

export default function App() {
  const auth = useAuth();
  const [showLog, setShowLog] = useState(false);

  useEffect(() => {
    void authController.restore();
  }, []);

  let content;
  if (showLog) content = <LogScreen onClose={() => setShowLog(false)} />;
  else if (auth.status === 'loading') content = <ActivityIndicator style={styles.fill} />;
  else if (auth.status === 'loggedOut') content = <LoginScreen message={auth.message} onShowLog={() => setShowLog(true)} />;
  else content = <Home key={auth.generation} onShowLog={() => setShowLog(true)} />;

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.fill} edges={['top', 'bottom']}>
        <StatusBar style="auto" />
        {content}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

/** The two WebView tabs (pages and chat, one web session) with the browser functions. */
function Home({ onShowLog }: { onShowLog: () => void }) {
  const [tab, setTab] = useState<Tab>('pages');
  const pages = useRef<WebScreenHandle>(null);
  const chat = useRef<WebScreenHandle>(null);
  const [states, setStates] = useState<Record<Tab, WebScreenState>>({
    pages: { canGoBack: false, canGoForward: false },
    chat: { canGoBack: false, canGoForward: false },
  });
  const current = tab === 'pages' ? pages : chat;
  const state = states[tab];

  return (
    <View style={styles.fill}>
      <View style={styles.toolbar}>
        <ToolButton label="◀" disabled={!state.canGoBack} onPress={() => current.current?.goBack()} />
        <ToolButton label="▶" disabled={!state.canGoForward} onPress={() => current.current?.goForward()} />
        <ToolButton label="⟳" onPress={() => current.current?.reload()} />
        <ToolButton label="Find" onPress={() => current.current?.openFind()} />
        <ToolButton label="Share" onPress={() => current.current?.share()} />
        <ToolButton label="Safari" onPress={() => current.current?.openInBrowser()} />
        <ToolButton label="Restart" onPress={() => current.current?.restart()} />
        <ToolButton label="Log" onPress={onShowLog} />
        <ToolButton label="Logout" onPress={() => void authController.logout()} />
      </View>
      <View style={[styles.fill, tab !== 'pages' && styles.hidden]}>
        <WebScreen
          ref={pages}
          active={tab === 'pages'}
          onNativePage={(page) => {
            if (page !== 'chat') return false;
            setTab('chat');
            return true;
          }}
          onStateChange={(it) => setStates((all) => ({ ...all, pages: it }))}
        />
      </View>
      <View style={[styles.fill, tab !== 'chat' && styles.hidden]}>
        <WebScreen
          ref={chat}
          initialPath="/next/chat"
          active={tab === 'chat'}
          onStateChange={(it) => setStates((all) => ({ ...all, chat: it }))}
        />
      </View>
      <View style={styles.tabs}>
        {(['pages', 'chat'] as Tab[]).map((it) => (
          <Pressable key={it} style={styles.tab} onPress={() => setTab(it)}>
            <Text style={[styles.tabText, tab === it && styles.tabActive]}>{it === 'pages' ? 'ProjectForge' : 'Chat'}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

function ToolButton({ label, onPress, disabled }: { label: string; onPress: () => void; disabled?: boolean }) {
  return (
    <Pressable onPress={onPress} disabled={disabled} style={styles.toolButton}>
      <Text style={[styles.toolText, disabled && styles.disabled]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  hidden: { display: 'none' },
  toolbar: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#999',
    paddingHorizontal: 4,
  },
  toolButton: { paddingHorizontal: 7, paddingVertical: 8 },
  toolText: { color: '#1976d2', fontSize: 14 },
  disabled: { color: '#bbb' },
  tabs: { flexDirection: 'row', borderTopWidth: StyleSheet.hairlineWidth, borderColor: '#999' },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 12 },
  tabText: { fontSize: 15, color: '#666' },
  tabActive: { color: '#1976d2', fontWeight: '600' },
});
