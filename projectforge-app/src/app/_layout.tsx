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

import { SplashScreen, Stack, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { authController, useAuth } from '../core/auth/authController';
import { t } from '../core/i18n/translations';
import { logUncaughtErrors } from '../core/log/uncaughtErrors';
import { appSettings } from '../core/settings/appSettings';
import { useColors } from '../core/ui/colors';
import { SnackbarHost } from '../core/ui/snackbar';

logUncaughtErrors();
void SplashScreen.preventAutoHideAsync();

/** The root: restores the login, then the tabs (logged in) or the login, and the screens opened from the menu. */
export default function RootLayout() {
  const auth = useAuth();
  const colors = useColors();

  useEffect(() => {
    appSettings.load();
    void authController.restore();
  }, []);

  useEffect(() => {
    if (auth.status !== 'restoring') void SplashScreen.hideAsync();
  }, [auth.status]);

  if (auth.status === 'restoring') return null;
  const loggedIn = auth.status === 'loggedIn';
  const modal = {
    headerShown: true,
    presentation: 'modal' as const,
    headerStyle: { backgroundColor: colors.surface },
    headerTintColor: colors.text,
  };
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StatusBar style="auto" />
        <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background } }}>
          <Stack.Screen name="index" />
          <Stack.Protected guard={loggedIn}>
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="card/[id]" />
            <Stack.Screen name="address/[id]" />
            <Stack.Screen name="booking" options={{ presentation: 'modal' }} />
            <Stack.Screen name="task-picker" options={{ presentation: 'modal' }} />
            <Stack.Screen name="notifications" options={{ ...modal, title: t('notification.center') }} />
            <Stack.Screen name="settings" options={{ ...modal, title: t('settings') }} />
            <Stack.Screen name="servers" options={{ ...modal, title: t('app.server.switch') }} />
            <Stack.Screen name="add-server" options={{ ...modal, title: t('app.server.add') }} />
          </Stack.Protected>
          <Stack.Protected guard={!loggedIn}>
            <Stack.Screen name="login" />
          </Stack.Protected>
          <Stack.Screen name="log" options={{ ...modal, title: t('app.log.title') }} />
        </Stack>
        <SnackbarHost />
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

/** Back from a screen of the menu; to the start if there is nothing to go back to (a deep link). */
export function useClose(): () => void {
  const router = useRouter();
  return () => (router.canGoBack() ? router.back() : router.replace('/'));
}
