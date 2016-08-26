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

package org.projectforge.continuousdb.demo.entities;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.Validate;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Table(name = "T_USER", uniqueConstraints = { @UniqueConstraint(columnNames = { "username"})})
public class UserDO extends DefaultBaseDO
{
  private String username;

  private String password;

  private boolean deactivated;

  private String description;

  private Timestamp lastLogin;

  private int loginFailures;

  private Locale locale;

  private Integer firstDayOfWeek;
  
  private Set<UserRightDO> rights = new HashSet<UserRightDO>();


  @Column
  public Locale getLocale()
  {
    return locale;
  }

  public void setLocale(final Locale locale)
  {
    this.locale = locale;
  }

  /**
   * 0 - sunday, 1 - monday etc.
   * @return the firstDayOfWeek
   */
  @Column(name = "first_day_of_week")
  public Integer getFirstDayOfWeek()
  {
    return firstDayOfWeek;
  }

  /**
   * @param firstDayOfWeek the firstDayOfWeek to set
   * @return this for chaining.
   */
  public UserDO setFirstDayOfWeek(final Integer firstDayOfWeek)
  {
    this.firstDayOfWeek = firstDayOfWeek;
    return this;
  }

  /**
   * @return Returns the username.
   */
  @Column(length = 255, unique = true, nullable = false)
  public String getUsername()
  {
    return username;
  }

  /**
   * @param username The username to set.
   * @return this for chaining.
   */
  public UserDO setUsername(final String username)
  {
    this.username = username;
    return this;
  }

  /**
   * Zeitstempel des letzten erfolgreichen Logins.
   * @return Returns the lastLogin.
   */
  @Column
  public Timestamp getLastLogin()
  {
    return lastLogin;
  }

  /**
   * @return Returns the description.
   */
  @Column(length = 255)
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description The description to set.
   * @return this for chaining.
   */
  public UserDO setDescription(final String description)
  {
    Validate.isTrue(description == null || description.length() <= 255, description);
    this.description = description;
    return this;
  }

  /**
   * Die Anzahl der erfolglosen Logins. Dieser Wert wird bei dem nächsten erfolgreichen Login auf 0 zurück gesetzt.
   * @return Returns the loginFailures.
   */
  @Column
  public int getLoginFailures()
  {
    return loginFailures;
  }

  /**
   * Encoded password of the user (SHA-1).
   * @return Returns the password.
   */
  @Column(length = 50)
  public String getPassword()
  {
    return password;
  }

  /**
   * @param password The password to set.
   * @return this for chaining.
   */
  public UserDO setPassword(final String password)
  {
    this.password = password;
    return this;
  }

  /**
   * @param lastLogin The lastLogin to set.
   */
  public void setLastLogin(final Timestamp lastLogin)
  {
    this.lastLogin = lastLogin;
  }

  /**
   * @param loginFailures The loginFailures to set.
   */
  public void setLoginFailures(final int loginFailures)
  {
    this.loginFailures = loginFailures;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user")
  public Set<UserRightDO> getRights()
  {
    return this.rights;
  }

  public void setRights(final Set<UserRightDO> rights)
  {
    this.rights = rights;
  }

  /**
   * A deactivated user has no more system access.
   * @return the deactivated
   */
  @Column(nullable = false)
  public boolean isDeactivated()
  {
    return deactivated;
  }

  /**
   * @param deactivated the deactivated to set
   * @return this for chaining.
   */
  public UserDO setDeactivated(final boolean deactivated)
  {
    this.deactivated = deactivated;
    return this;
  }
}
