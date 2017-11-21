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

import java.io.UnsupportedEncodingException;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.utils.ReflectionToString;

import de.micromata.genome.db.jpa.history.api.NoHistory;

/**
 * Scripts can be stored and executed by authorized users.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_SCRIPT", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_script_tenant_id", columnList = "tenant_id")
})
public class ScriptDO extends DefaultBaseDO
{
  public static final int PARAMETER_NAME_MAX_LENGTH = 100;
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ScriptDO.class);
  private static final long serialVersionUID = 7069806875752038860L;
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name; // 255 not null

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description; // 4000;

  @NoHistory
  private byte[] script;

  @NoHistory
  private byte[] scriptBackup;

  @NoHistory
  private byte[] file;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String filename;

  private String parameter1Name;

  private ScriptParameterType parameter1Type;

  private String parameter2Name;

  private ScriptParameterType parameter2Type;

  private String parameter3Name;

  private ScriptParameterType parameter3Type;

  private String parameter4Name;

  private ScriptParameterType parameter4Type;

  private String parameter5Name;

  private ScriptParameterType parameter5Type;

  private String parameter6Name;

  private ScriptParameterType parameter6Type;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * Please note: script is not historizable. Therefore there is now history of scripts.
   *
   * @return
   */
  @Basic(fetch = FetchType.LAZY)
  @Type(type = "binary")
  @Column(length = 2000)
  public byte[] getScript()
  {
    return script;
  }

  public void setScript(final byte[] script)
  {
    this.script = script;
  }

  private String convert(final byte[] bytes)
  {
    if (bytes == null) {
      return null;
    }
    String str = null;
    try {
      str = new String(bytes, "UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.fatal("Exception encountered while convering byte[] to String: " + ex.getMessage(), ex);
    }
    return str;
  }

  private byte[] convert(final String str)
  {
    if (str == null) {
      return null;
    }
    byte[] bytes = null;
    try {
      bytes = str.getBytes("UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.fatal("Exception encountered while convering String to bytes: " + ex.getMessage(), ex);
    }
    return bytes;
  }

  @Transient
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  public String getScriptAsString()
  {
    return convert(script);
  }

  public void setScriptAsString(final String script)
  {
    this.script = convert(script);
  }

  /**
   * Instead of historizing the script the last version of the script after changing it will stored in this field.
   *
   * @return
   */
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "script_backup", length = 2000)
  @Type(type = "binary")
  public byte[] getScriptBackup()
  {
    return scriptBackup;
  }

  public void setScriptBackup(final byte[] scriptBackup)
  {
    this.scriptBackup = scriptBackup;
  }

  @Transient
  public String getScriptBackupAsString()
  {
    return convert(scriptBackup);
  }

  public void setScriptBackupAsString(final String scriptBackup)
  {
    this.scriptBackup = convert(scriptBackup);
  }

  /**
   * @return the file
   */
  @Basic(fetch = FetchType.LAZY)
  @Column
  @Type(type = "binary")
  public byte[] getFile()
  {
    return file;
  }

  /**
   * @param file the file to set
   * @return this for chaining.
   */
  public void setFile(final byte[] file)
  {
    this.file = file;
  }

  /**
   * @return the filename
   */
  @Column(name = "file_name", length = 255)
  public String getFilename()
  {
    return filename;
  }

  /**
   * @param filename the filename to set
   * @return this for chaining.
   */
  public void setFilename(final String filename)
  {
    this.filename = filename;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public void setDescription(final String description)
  {
    this.description = description;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter1Name()
  {
    return parameter1Name;
  }

  public void setParameter1Name(final String parameter1Name)
  {
    this.parameter1Name = parameter1Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter1Type()
  {
    return parameter1Type;
  }

  public void setParameter1Type(final ScriptParameterType parameter1Type)
  {
    this.parameter1Type = parameter1Type;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter2Name()
  {
    return parameter2Name;
  }

  public void setParameter2Name(final String parameter2Name)
  {
    this.parameter2Name = parameter2Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter2Type()
  {
    return parameter2Type;
  }

  public void setParameter2Type(final ScriptParameterType parameter2Type)
  {
    this.parameter2Type = parameter2Type;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter3Name()
  {
    return parameter3Name;
  }

  public void setParameter3Name(final String parameter3Name)
  {
    this.parameter3Name = parameter3Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter3Type()
  {
    return parameter3Type;
  }

  public void setParameter3Type(final ScriptParameterType parameter3Type)
  {
    this.parameter3Type = parameter3Type;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter4Name()
  {
    return parameter4Name;
  }

  public void setParameter4Name(final String parameter4Name)
  {
    this.parameter4Name = parameter4Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter4Type()
  {
    return parameter4Type;
  }

  public void setParameter4Type(final ScriptParameterType parameter4Type)
  {
    this.parameter4Type = parameter4Type;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter5Name()
  {
    return parameter5Name;
  }

  public void setParameter5Name(final String parameter5Name)
  {
    this.parameter5Name = parameter5Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter5Type()
  {
    return parameter5Type;
  }

  public void setParameter5Type(final ScriptParameterType parameter5Type)
  {
    this.parameter5Type = parameter5Type;
  }

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Column(length = PARAMETER_NAME_MAX_LENGTH)
  public String getParameter6Name()
  {
    return parameter5Name;
  }

  public void setParameter6Name(final String parameter6Name)
  {
    this.parameter6Name = parameter6Name;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public ScriptParameterType getParameter6Type()
  {
    return parameter6Type;
  }

  public void setParameter6Type(final ScriptParameterType parameter6Type)
  {
    this.parameter6Type = parameter6Type;
  }

  @Transient
  public String getParameterNames(final boolean capitalize)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = appendParameterName(buf, parameter1Name, capitalize, true);
    first = appendParameterName(buf, parameter2Name, capitalize, first);
    first = appendParameterName(buf, parameter3Name, capitalize, first);
    first = appendParameterName(buf, parameter4Name, capitalize, first);
    first = appendParameterName(buf, parameter5Name, capitalize, first);
    first = appendParameterName(buf, parameter6Name, capitalize, first);
    return buf.toString();
  }

  private boolean appendParameterName(final StringBuffer buf, final String parameterName, final boolean capitalize,
      boolean first)
  {
    if (StringUtils.isNotBlank(parameterName) == true) {
      if (first == true) {
        first = false;
      } else {
        buf.append(", ");
      }
      if (capitalize == true) {
        buf.append(StringUtils.capitalize(parameterName));
      } else {
        buf.append(parameterName);
      }
    }
    return first;
  }

  /**
   * Returns string containing all fields (except the file) of given object (via ReflectionToStringBuilder).
   *
   * @param user
   * @return
   */
  @Override
  public String toString()
  {
    return (new ReflectionToString(this)
    {
      @Override
      protected boolean accept(final java.lang.reflect.Field f)
      {
        return super.accept(f) && !"file".equals(f.getName()) && !"script".equals(f.getName())
            && !"scriptBackup".equals(f.getName());
      }
    }).toString();
  }
}
