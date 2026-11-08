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

import { ReactNode, useEffect, useMemo, useState } from 'react';
import { Animated, BackHandler, Pressable, StyleSheet, useWindowDimensions, View } from 'react-native';
import { Gesture } from 'react-native-gesture-handler';

/** The gesture pulling the sheet down: attached by the content to its top (a handle, a title, tabs). */
export type SheetPull = ReturnType<typeof Gesture.Pan>;

/** How far (pt) or how fast (pt/s) the sheet must be pulled down to close. */
const CLOSE_DISTANCE = 120;
const CLOSE_VELOCITY = 800;
/** The page left visible above a sheet of the full height. */
const PAGE_PEEK = 44;

/**
 * A sheet moved up over the page of a tab (the tab bar stays below it), the page dimmed above it: a tap there, pulling
 * the sheet down by its top ([children] gets the gesture) or the back button of Android while [active] closes it
 * ([onClose]). [open]: shown (moved in and out); [fit]: as high as its content (at most three quarters), else up to
 * just below the top. [closeLabel]: the accessibility label of the dimmed page.
 */
export function TabSheet({
  open,
  active = true,
  fit = false,
  closeLabel,
  onClose,
  children,
}: {
  open: boolean;
  active?: boolean;
  fit?: boolean;
  closeLabel: string;
  onClose: () => void;
  children: (pull: SheetPull) => ReactNode;
}) {
  const { height } = useWindowDimensions();
  const [offset] = useState(() => new Animated.Value(height));
  const [shown, setShown] = useState(open);
  if (open && !shown) setShown(true);

  useEffect(() => {
    if (open) {
      Animated.spring(offset, { toValue: 0, useNativeDriver: true, bounciness: 0, speed: 18 }).start();
    } else {
      Animated.timing(offset, { toValue: height, duration: 200, useNativeDriver: true }).start(({ finished }) => {
        if (finished) setShown(false);
      });
    }
  }, [open, height, offset]);

  useEffect(() => {
    if (!open || !active) return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      onClose();
      return true;
    });
    return () => subscription.remove();
  }, [open, active, onClose]);

  // Pulled down by its top (the lists scroll themselves): far or fast enough closes it, else back up.
  const pull = useMemo(
    () =>
      Gesture.Pan()
        .runOnJS(true)
        .activeOffsetY(8)
        .failOffsetX([-24, 24])
        .onUpdate((event) => offset.setValue(Math.max(0, event.translationY)))
        .onEnd((event) => {
          if (event.translationY > CLOSE_DISTANCE || event.velocityY > CLOSE_VELOCITY) {
            onClose();
          } else {
            Animated.spring(offset, { toValue: 0, useNativeDriver: true, bounciness: 0 }).start();
          }
        }),
    [offset, onClose],
  );

  if (!shown) return null;
  const dim = offset.interpolate({ inputRange: [0, height], outputRange: [0.45, 0], extrapolate: 'clamp' });
  return (
    // Clipped: moving in and out, the sheet would be drawn over the tab bar below it.
    <View style={[StyleSheet.absoluteFill, styles.clip]} pointerEvents={open ? 'auto' : 'none'}>
      <Animated.View style={[StyleSheet.absoluteFill, styles.backdrop, { opacity: dim }]}>
        <Pressable style={styles.fill} accessibilityRole="button" accessibilityLabel={closeLabel} onPress={onClose} />
      </Animated.View>
      <Animated.View style={[fit ? styles.fit : styles.full, { transform: [{ translateY: offset }] }]}>
        {children(pull)}
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  clip: { overflow: 'hidden' },
  backdrop: { backgroundColor: '#000' },
  full: { position: 'absolute', top: PAGE_PEEK, left: 0, right: 0, bottom: 0 },
  fit: { position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '75%' },
});
