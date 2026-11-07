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

import { ComponentRef, ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import {
  Keyboard,
  KeyboardEvent,
  LayoutAnimation,
  Platform,
  StyleProp,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';

/**
 * A view filling its parent that leaves the keyboard free (iOS; Android resizes the window itself): its content ends
 * above the keyboard. RN's KeyboardAvoidingView compares the keyboard's position on the screen with its own layout,
 * which is relative to its parent: below a header, the safe area of the tabs (the Dynamic Island) or a banner it moves
 * up by that much too little. And an offset measured at its layout is wrong for a view that is moved afterwards (a
 * sheet sliding in is laid out off screen): the padding then took the whole height, a list above the search field got
 * none. So here the box is measured in the window whenever the keyboard comes or changes, and while it is shown.
 * [style]: of the box (e.g. its background).
 */
export function KeyboardAvoidingFill({ style, children }: { style?: StyleProp<ViewStyle>; children: ReactNode }) {
  const box = useRef<ComponentRef<typeof View>>(null);
  const keyboard = useRef<KeyboardEvent | null>(null);
  const [bottom, setBottom] = useState(0);

  /** The padding for the keyboard shown (none without), moving along with the keyboard for its [event]. */
  const update = useCallback((event?: KeyboardEvent) => {
    const animate = (next: number) => {
      if (event?.duration) {
        const duration = Math.max(event.duration, 10);
        LayoutAnimation.configureNext({
          duration,
          update: { duration, type: LayoutAnimation.Types[event.easing] ?? 'keyboard' },
        });
      }
      setBottom(next);
    };
    const shown = keyboard.current;
    if (shown == null) {
      animate(0);
      return;
    }
    box.current?.measureInWindow((_x, y, _width, height) =>
      animate(Math.max(0, y + height - shown.endCoordinates.screenY)),
    );
  }, []);

  useEffect(() => {
    if (Platform.OS !== 'ios') return;
    const change = (event: KeyboardEvent) => {
      keyboard.current = event;
      update(event);
    };
    const subscriptions = [
      Keyboard.addListener('keyboardWillShow', change),
      Keyboard.addListener('keyboardWillChangeFrame', change),
      Keyboard.addListener('keyboardWillHide', (event) => {
        keyboard.current = null;
        update(event);
      }),
    ];
    return () => subscriptions.forEach((it) => it.remove());
  }, [update]);

  return (
    // Measured without the padding (on the inner view), so it doesn't depend on itself.
    <View ref={box} style={[styles.fill, style]} onLayout={() => update()}>
      <View style={[styles.fill, { paddingBottom: bottom }]}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
});
