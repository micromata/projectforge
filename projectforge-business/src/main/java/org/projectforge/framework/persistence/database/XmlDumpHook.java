/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.persistence.database;

import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;
import org.projectforge.plugins.core.AbstractPlugin;

/**
 * Hooks for XmlDump.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Deprecated
public interface XmlDumpHook
{
  /**
   * Will be called before restoring an object. This method could be used for updating object id's etc. The registration
   * could be added to {@link InitDatabaseDao#setXmlDump(XmlDump)}. Plugins must not be registered because the method
   * {@link AbstractPlugin#onBeforeRestore(XStreamSavingConverter, Object)} will be called.
   * 
   * @param xstreamSavingConverter
   * @param obj
   */
  public void onBeforeRestore(UserXmlPreferencesDao userXmlPreferencesDao,
      final XStreamSavingConverter xstreamSavingConverter, final Object obj);

}
