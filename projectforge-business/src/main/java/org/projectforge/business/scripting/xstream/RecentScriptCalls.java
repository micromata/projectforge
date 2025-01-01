/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.projectforge.framework.utils.RecentQueue;


@XStreamAlias("RecentScriptCalls")
public class RecentScriptCalls
{
  private RecentQueue<ScriptCallData> recentQueue;

  public RecentScriptCalls()
  {
    recentQueue = new RecentQueue<>();
  }

  public void append(final ScriptCallData data)
  {
    recentQueue.append(data);
  }

  public ScriptCallData getScriptCallData(final String scriptName)
  {
    if (recentQueue.getRecent() == null) {
      return null;
    }
    for (final ScriptCallData data : recentQueue.getRecentList()) {
      if (scriptName.equals(data.getScriptName())) {
        return data;
      }
    }
    return null;
  }
}
