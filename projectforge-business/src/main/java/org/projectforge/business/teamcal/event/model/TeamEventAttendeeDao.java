package org.projectforge.business.teamcal.event.model;

import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.stereotype.Repository;

@Repository
public class TeamEventAttendeeDao extends BaseDao<TeamEventAttendeeDO>
{
  public TeamEventAttendeeDao()
  {
    super(TeamEventAttendeeDO.class);
  }

  @Override
  public TeamEventAttendeeDO newInstance()
  {
    return new TeamEventAttendeeDO();
  }
}
