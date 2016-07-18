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

package org.projectforge.framework.i18n;

import java.util.ResourceBundle;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.projectforge.framework.api.ProjectForgeException;

/**
 * This Exception will be thrown by the application and the message should be displayed.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class InternalErrorException extends ProjectForgeException
{
  private static final long serialVersionUID = 3659587126199090720L;

  protected String i18nKey = null;

  protected Object[] params;

  protected MessageParam[] msgParams;

  /**
   * @param i18nKey Key for the localized message.
   */
  @Deprecated
  public InternalErrorException()
  {
    this("exception.internalError");
  }

  /**
   * @param i18nKey Key for the localized message.
   */
  public InternalErrorException(String i18nKey)
  {
    super(i18nKey); // i18n key as message, if not displayed properly.
    this.i18nKey = i18nKey;
    this.params = null;
    this.msgParams = null;
  }

  /**
   * @param i18nKey Key for the localized message.
   * @param params Params, if message has params.
   */
  public InternalErrorException(String i18nKey, Object... params)
  {
    this(i18nKey);
    this.params = params;
  }

  /**
   * @param i18nKey Key for the localized message.
   * @param params Params, if message has params.
   */
  public InternalErrorException(String i18nKey, MessageParam... msgParams)
  {
    this(i18nKey);
    this.msgParams = msgParams;
  }

  /**
   * @return The key for the localized message.
   */
  public String getI18nKey()
  {
    return i18nKey;
  }

  /**
   * The i18n params if set.
   */
  public MessageParam[] getMsgParams()
  {
    return msgParams;
  }

  /**
   * @return The params for the localized message if exist, otherwise null.
   */
  public Object[] getParams()
  {
    return params;
  }

  /**
   * @param bundle
   * @return The params for the localized message if exist (prepared for using with MessageFormat), otherwise params
   *         will be returned.
   */
  public Object[] getParams(ResourceBundle bundle)
  {
    if (msgParams == null) {
      return params;
    }
    Object[] args = new Object[msgParams.length];
    for (int i = 0; i < msgParams.length; i++) {
      if (msgParams[i].isI18nKey() == true) {
        args[i] = bundle.getString(msgParams[i].getI18nKey());
      } else {
        args[i] = msgParams[i];
      }
    }
    return args;
  }

  @Override
  public String toString()
  {
    ToStringBuilder builder = new ToStringBuilder(this);
    if (i18nKey != null) {
      builder.append("i18nKey", i18nKey);
    }
    if (params != null) {
      builder.append("params", params);
    }
    if (msgParams != null) {
      builder.append("msgParams", msgParams);
    }
    return builder.toString();
  }
}
