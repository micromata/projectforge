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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.projectforge.AppVersion;
import org.projectforge.business.fibu.kost.reporting.ReportGeneratorList;
import org.projectforge.business.task.ScriptingTaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class ScriptDao extends BaseDao<ScriptDO>
{
  @Autowired
  private GroovyExecutor groovyExecutor;

  private GroovyResult groovyResult;

  public ScriptDao()
  {
    super(ScriptDO.class);
  }

  /**
   * Copy old script as script backup if modified.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(org.projectforge.core.ExtendedBaseDO,
   * org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onChange(final ScriptDO obj, final ScriptDO dbObj)
  {
    if (Arrays.equals(dbObj.getScript(), obj.getScript()) == false) {
      obj.setScriptBackup(dbObj.getScript());
    }
  }

  /**
   * User must be member of group controlling or finance.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final ScriptDO obj, final ScriptDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.FINANCE_GROUP);
  }

  @Override
  public ScriptDO newInstance()
  {
    return new ScriptDO();
  }

  public GroovyResult execute(final ScriptDO script, final List<ScriptParameter> parameters)
  {
    hasLoggedInUserSelectAccess(script, true);
    final ReportGeneratorList reportGeneratorList = new ReportGeneratorList();
    final Map<String, Object> scriptVariables = new HashMap<String, Object>();

    addScriptVariables(scriptVariables);
    scriptVariables.put("reportList", reportGeneratorList);
    if (parameters != null) {
      for (final ScriptParameter param : parameters) {
        scriptVariables.put(param.getParameterName(), param.getValue());
      }
    }
    if (script.getFile() != null) {
      final Map<String, Object> scriptVars = new HashMap<String, Object>();
      scriptVariables.put("script", scriptVars);
      scriptVars.put("file", script.getFile());
      scriptVars.put("filename", script.getFilename());
    }

    scriptVariables.put("i18n", new I18n());

    String scriptContent = script.getScriptAsString();
    if (scriptContent.contains("import org.projectforge.export")) {
      // Package was renamed in version 5.2 and 6.13:
      scriptContent = scriptContent.replace("import org.projectforge.export",
          "import org.projectforge.export.*\nimport org.projectforge.business.excel");
    }
    groovyResult = groovyExecutor.execute(new GroovyResult(), scriptContent, scriptVariables);
    return groovyResult;
  }

  /**
   * Adds all registered dao's and other variables, such as appId, appVersion and task-tree. These variables are
   * available in Groovy scripts
   */
  public void addScriptVariables(final Map<String, Object> scriptVariables)
  {
    scriptVariables.put("appId", AppVersion.APP_ID);
    scriptVariables.put("appVersion", AppVersion.NUMBER);
    scriptVariables.put("appRelease", AppVersion.RELEASE_DATE);
    scriptVariables.put("reportList", null);
    scriptVariables.put("taskTree", new ScriptingTaskTree(TaskTreeHelper.getTaskTree()));
    for (final RegistryEntry entry : Registry.getInstance().getOrderedList()) {
      final ScriptingDao<?> scriptingDao = entry.getScriptingDao();
      if (scriptingDao != null) {
        final String varName = StringUtils.uncapitalize(entry.getId());
        scriptVariables.put(varName + "Dao", scriptingDao);
      }
    }
  }

}
