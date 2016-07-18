package org.projectforge.web.selenium.administration;

import org.projectforge.web.selenium.EditPage;

public class SeleniumGroupEditPage extends EditPage<SeleniumGroupEditPage>
{
  public SeleniumGroupEditPage setName(String value) {
    return setStringElementById("name", value);
  }

  public String getName() {
    return getStringElementById("name");
  }

  public SeleniumGroupEditPage setOrganization(String value) {
    return setStringElementById("organization", value);
  }

  public String getOrganization() {
    return getStringElementById("organization");
  }

  public SeleniumGroupEditPage setDescription(String value) {
    return setStringElementById("description", value);
  }

  @Override
  protected String urlPostfix()
  {
    return "wa/groupEdit?";
  }
}
