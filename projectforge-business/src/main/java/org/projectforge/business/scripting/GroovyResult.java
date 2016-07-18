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

package org.projectforge.business.scripting;

import java.io.Serializable;

import org.projectforge.business.utils.HtmlHelper;

public class GroovyResult implements Serializable
{
  private static final long serialVersionUID = -4561647483563741849L;

  private transient Object result;

  private transient Exception exception;

  private String output;

  public GroovyResult()
  {
  }

  public GroovyResult(final Exception ex)
  {
    this.exception = ex;
  }

  public void setResult(final Object result)
  {
    this.result = result;
  }

  public boolean hasResult()
  {
    return result != null;
  }

  public Object getResult()
  {
    return result;
  }

  /**
   * Escapes all html characters. If Groovy result is from type string then all '\n' will be replaced by "<br/>\n".
   * 
   * @return
   */
  public String getResultAsHtmlString()
  {
    if (result == null) {
      return null;
    }
    final String esc = HtmlHelper.escapeHtml(result.toString(), true);
    return esc;
  }

  public boolean hasException()
  {
    return exception != null;
  }

  public Exception getException()
  {
    return exception;
  }

  /**
   * @param exception the exception to set
   * @return this for chaining.
   */
  public GroovyResult setException(final Exception exception)
  {
    this.exception = exception;
    return this;
  }

  public String getOutput()
  {
    return output;
  }

  public void setOutput(final String output)
  {
    this.output = output;
  }
}
