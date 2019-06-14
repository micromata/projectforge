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

package org.projectforge.business.login;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum PasswordCheckResult
{
  /** Password check failed. */
  FAILED,
  /** Password checked successfully (without salt, password has to be salted!). */
  OK_WITHOUT_SALT,
  /** Password checked successfully (without pepper but pepper is given). */
  OK_WITHOUT_PEPPER,
  /** Password checked successfully (without salt and pepper, please give pepper and salt to the password). */
  OK_WITHOUT_SALT_AND_PEPPER,
  /** Password checked successfully and password is salted (and pepper is given if configured). Nothing to be done. */
  OK;

  /**
   * @return True if the password check was successfully. A password update is may-be needed, please call {@link #isPasswordUpdateNeeded()} to
   *         check this.
   */
  public boolean isOK()
  {
    return this != FAILED;
  }

  public boolean isPasswordUpdateNeeded()
  {
    return this == OK_WITHOUT_SALT || this == OK_WITHOUT_PEPPER || this == OK_WITHOUT_SALT_AND_PEPPER;
  }
}
