package org.projectforge.web.selenium.teamcal;

import org.projectforge.web.selenium.TestPageBase;

public class SeleniumTeamCalPage extends TestPageBase<SeleniumTeamCalPage>
{



  @Override
  public String getUrlPostfix()
  {
    return "wa/teamCalendar?";
  }

}
