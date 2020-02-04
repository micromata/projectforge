/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.utils.ReplaceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.AppVersion;
import org.projectforge.business.fibu.kost.reporting.ReportGeneratorList;
import org.projectforge.business.task.ScriptingTaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class ScriptDao extends BaseDao<ScriptDO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScriptDao.class);

  @Autowired
  private GroovyExecutor groovyExecutor;

  public ScriptDao()
  {
    super(ScriptDO.class);
  }

  /**
   * Copy old script as script backup if modified.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected void onChange(final ScriptDO obj, final ScriptDO dbObj)
  {
    if (!Arrays.equals(dbObj.getScript(), obj.getScript())) {
      obj.setScriptBackup(dbObj.getScript());
      final String filename = ReplaceUtils.encodeFilename(dbObj.getName() + "_" + PFDateTime.now().getIsoStringSeconds() + ".groovy", true);
      final File backupDir = new File(ConfigXml.getInstance().getBackupDirectory(), "scripts");
      ConfigXml.ensureDir(backupDir);
      final File file = new File(backupDir, filename);
      try {
        log.info("Writing backup of script to: " + file.getAbsolutePath());
        FileUtils.writeStringToFile(file, dbObj.getScriptAsString());
      }catch (IOException ex) {
        log.error("Error while trying to save backup file of script '" + file.getAbsolutePath() + "': " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * User must be member of group controlling or finance.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasDeleteAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, boolean)
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

  public ScriptExecutionResult execute(final ScriptDO script, final List<ScriptParameter> parameters)
  {
    hasLoggedInUserSelectAccess(script, true);
    final ReportGeneratorList reportGeneratorList = new ReportGeneratorList();
    final Map<String, Object> scriptVariables = new HashMap<>();

    addScriptVariables(scriptVariables);
    scriptVariables.put("reportList", reportGeneratorList);
    if (parameters != null) {
      for (final ScriptParameter param : parameters) {
        scriptVariables.put(param.getParameterName(), param.getValue());
      }
    }
    if (script.getFile() != null) {
      final Map<String, Object> scriptVars = new HashMap<>();
      scriptVariables.put("script", scriptVars);
      scriptVars.put("file", script.getFile());
      scriptVars.put("filename", script.getFilename());
    }

    scriptVariables.put("i18n", new I18n());

    String scriptContent = script.getScriptAsString();
    if (script.getType() == ScriptDO.ScriptType.KOTLIN) {
      return KotlinScriptExecutor.execute(scriptContent, scriptVariables, script.getFile(), script.getFilename());
    }
    if (scriptContent.contains("import org.projectforge.export")) {
      // Package was renamed in version 5.2 and 6.13:
      scriptContent = scriptContent.replace("import org.projectforge.export",
          "import org.projectforge.export.*\nimport org.projectforge.business.excel");
    }
    return groovyExecutor.execute(new ScriptExecutionResult(), scriptContent, scriptVariables);
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
