/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.projectforge.framework.xstream.XmlObject;

/**
 * Stores the user interface status of an invoice (such as opened/closed invoice positions). This class will be marshalled and unmarshalled
 * as XML.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "rechnungUIStatus")
public class RechnungUIStatus implements Serializable
{
  private static final long serialVersionUID = -631242690490422127L;

  private Set<Short> closedPositions = null; // Can't be final, otherwise XmlObjectWriter doesn't work.

  public RechnungUIStatus()
  {
    closedPositions = new HashSet<Short>();
  }

  public RechnungUIStatus openPosition(final short pos)
  {
    closedPositions.remove(pos);
    return this;
  }

  public RechnungUIStatus closePosition(final short pos)
  {
    closedPositions.add(pos);
    return this;
  }

  public boolean isOpened(final short pos)
  {
    return !isClosed(pos);
  }

  public boolean isClosed(final short pos)
  {
    return closedPositions.contains(pos);
  }
}
