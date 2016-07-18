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

package de.micromata.wicket.ajax;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;

/**
 * Own {@link AbstractDefaultAjaxBehavior}, just to make getCallbackScript public.<br/>
 * 
 * 
 * @author <a href="mailto:j.unterstein@micromata.de">Johannes Unterstein</a>
 * @see Wicket 6.0.0
 */
public abstract class MDefaultAjaxBehavior extends AbstractDefaultAjaxBehavior
{

  private static final long serialVersionUID = 5092956245673681067L;

  @Override
  public CharSequence getCallbackScript()
  {
    return super.getCallbackScript();
  }
}
