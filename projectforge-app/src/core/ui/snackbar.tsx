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

import { useEffect, useSyncExternalStore } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useColors } from './colors';

export interface SnackbarMessage {
  text: string;
  /** A button of the message (e.g. undo). */
  action?: { label: string; onPress: () => void };
}

const DURATION_MILLIS = 4000;
const DURATION_WITH_ACTION_MILLIS = 7000;

/** Short messages at the bottom of the screen (booked, deleted with undo), one at a time: a new one replaces it. */
class Snackbar {
  private message: (SnackbarMessage & { key: number }) | null = null;
  private counter = 0;
  private listeners = new Set<() => void>();

  show(message: SnackbarMessage | string) {
    this.message = { ...(typeof message === 'string' ? { text: message } : message), key: ++this.counter };
    this.emit();
  }

  hide(key?: number) {
    if (key != null && this.message?.key !== key) return;
    this.message = null;
    this.emit();
  }

  get = () => this.message;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

export const snackbar = new Snackbar();

/** Shows the messages of [snackbar], above the tab bar (once in the root layout). */
export function SnackbarHost() {
  const message = useSyncExternalStore(snackbar.subscribe, snackbar.get);
  const colors = useColors();
  const insets = useSafeAreaInsets();
  useEffect(() => {
    if (message == null) return;
    const timeout = setTimeout(
      () => snackbar.hide(message.key),
      message.action ? DURATION_WITH_ACTION_MILLIS : DURATION_MILLIS,
    );
    return () => clearTimeout(timeout);
  }, [message]);
  if (message == null) return null;
  return (
    <View pointerEvents="box-none" style={[styles.host, { bottom: insets.bottom + 56 }]}>
      <View style={[styles.bar, { backgroundColor: colors.text }]} accessibilityLiveRegion="polite">
        <Text style={[styles.text, { color: colors.background }]}>{message.text}</Text>
        {message.action && (
          <Pressable
            hitSlop={8}
            onPress={() => {
              snackbar.hide(message.key);
              message.action?.onPress();
            }}
          >
            <Text style={[styles.action, { color: colors.primary }]}>{message.action.label}</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  host: { position: 'absolute', left: 12, right: 12 },
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    shadowColor: '#000',
    shadowOpacity: 0.25,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 4,
  },
  text: { flex: 1, fontSize: 14 },
  action: { fontSize: 14, fontWeight: '700' },
});
