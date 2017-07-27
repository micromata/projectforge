package org.projectforge.business.common;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * Created by blumenstein on 18.07.17.
 */
@MappedSuperclass
public class BaseUserGroupRightsDO extends DefaultBaseDO
{
  private String fullAccessGroupIds, fullAccessUserIds;

  private String readonlyAccessGroupIds, readonlyAccessUserIds;

  private String minimalAccessGroupIds, minimalAccessUserIds;

  /**
   * Members of these groups have full read/write access to all entries of this object.
   *
   * @return the fullAccessGroupIds
   */
  @Column(name = "full_access_group_ids", nullable = true)
  public String getFullAccessGroupIds()
  {
    return fullAccessGroupIds;
  }

  /**
   * These users have full read/write access to all entries of this object.
   *
   * @param fullAccessGroupIds the fullAccessGroupIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setFullAccessGroupIds(final String fullAccessGroupIds)
  {
    this.fullAccessGroupIds = fullAccessGroupIds;
    return this;
  }

  /**
   * @return the fullAccessUserIds
   */
  @Column(name = "full_access_user_ids", nullable = true)
  public String getFullAccessUserIds()
  {
    return fullAccessUserIds;
  }

  /**
   * @param fullAccessUserIds the fullAccessUserIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setFullAccessUserIds(final String fullAccessUserIds)
  {
    this.fullAccessUserIds = fullAccessUserIds;
    return this;
  }

  /**
   * Members of these groups have full read-only access to all entries of this object.
   *
   * @return the readonlyAccessGroupIds
   */
  @Column(name = "readonly_access_group_ids", nullable = true)
  public String getReadonlyAccessGroupIds()
  {
    return readonlyAccessGroupIds;
  }

  /**
   * @param readonlyAccessGroupIds the readonlyAccessGroupIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setReadonlyAccessGroupIds(final String readonlyAccessGroupIds)
  {
    this.readonlyAccessGroupIds = readonlyAccessGroupIds;
    return this;
  }

  /**
   * These users have full read-only access to all entries of this object.
   *
   * @return the readonlyAccessUserIds
   */
  @Column(name = "readonly_access_user_ids", nullable = true)
  public String getReadonlyAccessUserIds()
  {
    return readonlyAccessUserIds;
  }

  /**
   * @param readonlyAccessUserIds the readonlyAccessUserIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setReadonlyAccessUserIds(final String readonlyAccessUserIds)
  {
    this.readonlyAccessUserIds = readonlyAccessUserIds;
    return this;
  }

  /**
   * Members of these group have read-only access to all entries of this object, but they can only see the event start
   * and stop time
   *
   * @return the minimalAccessGroupIds
   */
  @Column(name = "minimal_access_group_ids", nullable = true)
  public String getMinimalAccessGroupIds()
  {
    return minimalAccessGroupIds;
  }

  /**
   * @param minimalAccessGroupIds the minimalAccessGroupIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setMinimalAccessGroupIds(final String minimalAccessGroupIds)
  {
    this.minimalAccessGroupIds = minimalAccessGroupIds;
    return this;
  }

  /**
   * Members of this group have only access to the start and stop time, nothing else.
   *
   * @return the minimalAccessUserIds
   */
  @Column(name = "minimal_access_user_ids", nullable = true)
  public String getMinimalAccessUserIds()
  {
    return minimalAccessUserIds;
  }

  /**
   * @param minimalAccessUserIds the minimalAccessUserIds to set
   * @return this for chaining.
   */
  public BaseUserGroupRightsDO setMinimalAccessUserIds(final String minimalAccessUserIds)
  {
    this.minimalAccessUserIds = minimalAccessUserIds;
    return this;
  }
}
