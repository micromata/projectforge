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

import { Text, View } from 'react-native';

import { useColors } from '../../core/ui/colors';
import { TaskRef } from './timesheet';

/** The parts of [text], each marked if it is part of one of [words] (lower case). */
export function highlight(text: string, words: string[]): { text: string; marked: boolean }[] {
  const lower = text.toLowerCase();
  const marked = new Array<boolean>(text.length).fill(false);
  for (const word of words.filter((it) => it.length > 0)) {
    let i = lower.indexOf(word);
    while (i >= 0) {
      marked.fill(true, i, i + word.length);
      i = lower.indexOf(word, i + word.length);
    }
  }
  const parts: { text: string; marked: boolean }[] = [];
  let start = 0;
  for (let i = 1; i <= text.length; i++) {
    if (i === text.length || marked[i] !== marked[start]) {
      parts.push({ text: text.substring(start, i), marked: marked[start] ?? false });
      start = i;
    }
  }
  return parts;
}

/** The title of a task, the path above it in small letters (as the web shows it), [words] of a search marked. */
export function TaskLabel({ task, words = [] }: { task: TaskRef; words?: string[] }) {
  const colors = useColors();
  const marked = (text: string) =>
    highlight(text, words).map((it, i) => (
      <Text key={i} style={it.marked ? { fontWeight: '700', color: colors.primary } : undefined}>
        {it.text}
      </Text>
    ));
  return (
    <View>
      {task.parents.length > 0 && (
        <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={2}>
          {marked(task.parents.join(' › '))}
        </Text>
      )}
      <Text style={{ color: colors.text, fontWeight: '600' }}>{marked(task.title)}</Text>
    </View>
  );
}
