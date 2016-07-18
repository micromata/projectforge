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

package org.projectforge.rest.objects;

import java.lang.reflect.Field;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.rest.AbstractBaseObject;

/**
 * For documentation please refer the ProjectForge-API: PFUserDO object.
 * REST object user.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserObject extends AbstractBaseObject
{
  private String username, firstName, lastName, email;

  private String authenticationToken;

  private String timeZone, locale;

  public UserObject()
  {
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(final String username)
  {
    this.username = username;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public void setFirstName(final String firstName)
  {
    this.firstName = firstName;
  }

  public String getLastName()
  {
    return lastName;
  }

  public void setLastName(final String lastName)
  {
    this.lastName = lastName;
  }

  public String getEmail()
  {
    return email;
  }

  public void setEmail(final String email)
  {
    this.email = email;
  }

  public String getAuthenticationToken()
  {
    return authenticationToken;
  }

  public void setAuthenticationToken(final String authenticationToken)
  {
    this.authenticationToken = authenticationToken;
  }

  /**
   * @return the timeZone
   */
  public String getTimeZone()
  {
    return timeZone;
  }

  /**
   * @param timeZone the timeZone to set
   * @return this for chaining.
   */
  public UserObject setTimeZone(final String timeZone)
  {
    this.timeZone = timeZone;
    return this;
  }

  /**
   * @return the locale
   */
  public String getLocale()
  {
    return locale;
  }

  /**
   * @param locale the locale to set
   * @return this for chaining.
   */
  public UserObject setLocale(final String locale)
  {
    this.locale = locale;
    return this;
  }

  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this) {
      @Override
      protected boolean accept(final Field f)
      {
        return super.accept(f) && !f.getName().equals("authenticationToken");
      }
    }.toString();
  }
}
