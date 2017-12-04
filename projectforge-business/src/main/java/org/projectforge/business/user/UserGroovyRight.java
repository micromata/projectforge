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

package org.projectforge.business.user;

import java.io.Serializable;

import org.projectforge.business.scripting.GroovyExecutor;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import groovy.lang.Binding;
import groovy.lang.Script;

/**
 * Rights which can be implemented dynamically with Groovy scripts.<br/>
 * PLEASE NOTE: The running of the script is synchronized! Therefore this class should not be used for long running scripts!
 * <br/>
 * This class is unused yet.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserGroovyRight extends UserRight implements Serializable
{
  private static final long serialVersionUID = 8001414492148781276L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserGroovyRight.class);

  private static final GroovyExecutor groovyExecutor = new GroovyExecutor();

  private Script groovyScript;

  /**
   * The Groovy script should return true or false. Inside the script the variables "userGroupCache", "user" and "value" are given as bind
   * variables.
   * @param id
   * @param category
   * @param groovyScript
   */
  public UserGroovyRight(final UserRightId id, final UserRightCategory category, final String groovyScript)
  {
    super(id, category);
    this.groovyScript = groovyExecutor.compileGroovy(groovyScript, false);
  }

  /**
   * PLEASE NOTE: This block is synchronized! Therefore this class should not be used for long running scripts!
   * @return Calls the Groovy script.
   */
  @Override
  public boolean matches(final UserGroupCache userGroupCache, final PFUserDO user, final UserRightValue value)
  {
    synchronized (groovyScript) {
      final Binding binding = groovyScript.getBinding();
      binding.setVariable("userGroupCache", userGroupCache);
      binding.setVariable("user", user);
      binding.setVariable("value", value);
      try {
        return (Boolean) groovyScript.run();
      } catch (Exception ex) {
        log.error("Groovy-Execution-Exception: " + ex.getMessage(), ex);
        return false;
      }
    }
  }
}
