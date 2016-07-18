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

package org.projectforge.framework.persistence.entities;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * Declares lastUpdate and created as invalidHistorizableProperties.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@MappedSuperclass
@WithHistory(noHistoryProperties = { "lastUpdate", "created" })
public abstract class AbstractHistorizableBaseDO<I extends Serializable>extends AbstractBaseDO<I>
{
  private static final long serialVersionUID = -5980671510045450615L;

}
