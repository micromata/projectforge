package org.projectforge.caldav.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import io.milton.annotations.Name;

@Entity(name = "t_pf_user")
public class User
{
  @Id
  private Long pk;

  @Column(name = "username")
  private String username;

  @Column(name = "deleted")
  private Boolean deleted;

  @Column(name = "authentication_token")
  private String authenticationToken;

  public Long getPk()
  {
    return pk;
  }

  public void setPk(Long pk)
  {
    this.pk = pk;
  }

  @Name
  public String getUsername()
  {
    return this.username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public Boolean getDeleted()
  {
    return deleted;
  }

  public void setDeleted(Boolean deleted)
  {
    this.deleted = deleted;
  }

  public String getAuthenticationToken()
  {
    return authenticationToken;
  }

  public void setAuthenticationToken(String authenticationToken)
  {
    this.authenticationToken = authenticationToken;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return pk != null ? pk.equals(user.pk) : user.pk == null;
  }

  @Override
  public int hashCode()
  {
    return pk != null ? 42 * pk.hashCode() : 0;
  }

  @Override
  public String toString()
  {
    return String.format("User[id=%d, username='%s']", pk, username);
  }
}
