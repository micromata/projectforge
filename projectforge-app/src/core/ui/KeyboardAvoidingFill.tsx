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

import { ComponentRef, ReactNode, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, View } from 'react-native';

/**
 * A KeyboardAvoidingView filling its parent, also below the top of the window. The view compares the keyboard's
 * position on the screen with its own layout, which is relative to its parent: below a header, the safe area of the
 * tabs (the Dynamic Island) or a banner it moves up by that much too little, and a field at its bottom stays behind
 * the keyboard. Here its offset in the window is measured and passed on.
 */
export function KeyboardAvoidingFill({ children }: { children: ReactNode }) {
  const box = useRef<ComponentRef<typeof View>>(null);
  const [offset, setOffset] = useState(0);
  return (
    <View ref={box} style={styles.fill} onLayout={() => box.current?.measureInWindow((_x, y) => setOffset(y))}>
      <KeyboardAvoidingView
        style={styles.fill}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={offset}
      >
        {children}
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
});
