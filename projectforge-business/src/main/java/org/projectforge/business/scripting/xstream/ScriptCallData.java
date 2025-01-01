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
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.scripting.ScriptParameter;

import java.util.List;

/**
 * For storing the user's last script calls. The parameters will be stored per script.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("ScriptCall")
public class ScriptCallData
{
  /**
   * Since React version of scripting execution page, the id of the script is used as scriptName.
   */
  @XStreamAsAttribute
  private String scriptName;

  @XStreamImplicit(itemFieldName = "parameter")
  private List<ScriptParameter> scriptParameters;

  public ScriptCallData()
  {
  }

  public ScriptCallData(final String name)
  {
    this(name, null);
  }

  public ScriptCallData(final String name, final List<ScriptParameter> scriptParameters)
  {
    this.scriptName = name;
    this.scriptParameters = scriptParameters;
  }

  public String getScriptName()
  {
    return scriptName;
  }

  public void setScriptName(final String scriptName)
  {
    this.scriptName = scriptName;
  }

  public List<ScriptParameter> getScriptParameter()
  {
    return scriptParameters;
  }

  public void setScriptParameter(final List<ScriptParameter> scriptParameter)
  {
    this.scriptParameters = scriptParameter;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ScriptCallData) {
      return StringUtils.equals(scriptName, ((ScriptCallData) obj).scriptName);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((scriptName == null) ? 0 : scriptName.hashCode());
    result = prime * result + ((scriptParameters == null) ? 0 : scriptParameters.hashCode());
    return result;
  }

}
