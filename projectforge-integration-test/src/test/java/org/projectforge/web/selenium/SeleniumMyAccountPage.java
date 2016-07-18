package org.projectforge.web.selenium;

import org.projectforge.web.selenium.fibu.AddressAndBankingEditingPage;

public class SeleniumMyAccountPage extends AddressAndBankingEditingPage<SeleniumMyAccountPage>
{


  public SeleniumMyAccountPage setFirstName(String firstName) {
    setStringElementById("firstName",firstName);
    return this;
  }

  public SeleniumMyAccountPage setLastName(String lastName) {
    setStringElementById("lastName",lastName);
    return this;
  }

  public SeleniumMyAccountPage setOrganization(String organization) {
    setStringElementById("organization",organization);
    return this;
  }

  public SeleniumMyAccountPage setEmail(String email) {
    setStringElementById("email", email);
    return this;
  }

  public SeleniumMyAccountPage setJiraUsername(String jiraUsername) {
    setStringElementById("jiraUsername", jiraUsername);
    return this;
  }

  public SeleniumMyAccountPage setDescription(String description) {
    setStringElementById("description",description);
    return this;
  }

  public SeleniumMyAccountPage setSSHPublicKey(String sshPublicKey) {
    setStringElementById("sshPublicKey", sshPublicKey);
    return this;
  }

  public SeleniumMyAccountPage update() {
    clickAndWaitForFullPageReload("update");
    return this;
  }

  @Override protected String urlPostfix()
  {
    return "wa/myAccount?";
  }
}
