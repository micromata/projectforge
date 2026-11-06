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

import { useCallback } from 'react';
import { View } from 'react-native';

import { useIsFocused } from 'expo-router';
import { AlertBanner } from '../../features/home/AlertBanner';
import { WebScreen } from '../../features/web/WebScreen';
import { CHAT_PATH, webTabs } from '../../features/web/webTabs';

/**
 * The chat of next in a WebView of its own (same web session as the pages): native screens would have to repeat
 * every feature of it. The badge of unread messages is at its tab.
 */
export default function ChatTab() {
  const focused = useIsFocused();
  const register = useCallback((handle: Parameters<typeof webTabs.register>[1]) => webTabs.register('chat', handle), []);
  return (
    <View style={{ flex: 1 }}>
      <AlertBanner />
      <WebScreen
        ref={register}
        initialPath={webTabs.takePending('chat') ?? CHAT_PATH}
        active={focused}
        onStateChange={(state) => webTabs.setState('chat', state)}
      />
    </View>
  );
}
