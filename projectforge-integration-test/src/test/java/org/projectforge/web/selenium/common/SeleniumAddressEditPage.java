package org.projectforge.web.selenium.common;

import org.projectforge.web.selenium.EditPage;

public class SeleniumAddressEditPage extends EditPage<SeleniumAddressEditPage>
{
  public static final String FORM_MISTER = "MISTER";
  public static final String FORM_MISS = "MISS";
  public static final String FORM_COMPANY = "COMPANY";
  public static final String FORM_MISC = "MISC";
  public static final String FORM_UNKNOWN = "UNKNOWN";
  public static final String STATE_UPTODATE = "UPTODATE";
  public static final String STATE_OUTDATED = "OUTDATED";
  public static final String STATE_LEAVED = "LEAVED";

  @Override
  protected String urlPostfix()
  {
    return "wa/addressEdit?";
  }

  public SeleniumAddressEditPage setOderbook(String value)
  {
    return chooseOptionsOfSelect2("addressbook-select", value);
  }

  public SeleniumAddressEditPage setName(String value)
  {
    return setStringElementById("name", value);
  }

  public SeleniumAddressEditPage setFirstName(String value)
  {
    return setStringElementById("firstName", value);
  }

  public SeleniumAddressEditPage setOrganization(String value)
  {
    return setStringElementById("organization", value);
  }

  public SeleniumAddressEditPage setDivision(String value)
  {
    return setStringElementById("division", value);
  }

  public SeleniumAddressEditPage setPosition(String value)
  {
    return setStringElementById("position", value);
  }

  public SeleniumAddressEditPage setEmail(String value)
  {
    return setStringElementById("email", value);
  }

  public SeleniumAddressEditPage setTitle(String value)
  {
    return setStringElementById("title", value);
  }

  public SeleniumAddressEditPage setWebsite(String value)
  {
    return setStringElementById("website", value);
  }

  public SeleniumAddressEditPage setPrivateEmail(String value)
  {
    return setStringElementById("privateEmail", value);
  }

  public SeleniumAddressEditPage setFingerprint(String value)
  {
    return setStringElementById("fingerprint", value);
  }

  public SeleniumAddressEditPage setPublicKey(String value)
  {
    return setStringElementById("publicKey", value);
  }

  public SeleniumAddressEditPage setBirthday(String value)
  {
    return setStringElementById("birthday", value);
  }

  public SeleniumAddressEditPage setLanguage(String value)
  {
    return setStringElementById("language", value);
  }

  public SeleniumAddressEditPage setForm(String value)
  {
    return selectDropdownListByValue("form", value);
  }

  public SeleniumAddressEditPage setContactStatus(String value)
  {
    return selectDropdownListByValue("contactStatus", value);
  }

  public SeleniumAddressEditPage setAddressStatus(String value)
  {
    return selectDropdownListByValue("addressStatus", value);
  }

  public String getName()
  {
    return getStringElementById("name");
  }

  public String getFirstName()
  {
    return getStringElementById("firstName");
  }

  public String getOrganization()
  {
    return getStringElementById("organization");
  }

  public String getDivision()
  {
    return getStringElementById("division");
  }

  public String getPosition()
  {
    return getStringElementById("position");
  }

  public String getEmail()
  {
    return getStringElementById("email");
  }

  public String getTitle()
  {
    return getStringElementById("title");
  }

  public String getPrivateEmail()
  {
    return getStringElementById("privateEmail");
  }

  public String getWebsite()
  {
    return getStringElementById("website");
  }

  public String getBirthday()
  {
    return getStringElementById("birthday");
  }

  public String getFingerprint()
  {
    return getStringElementById("fingerprint");
  }

  public String getPublicKey()
  {
    return getStringElementById("publicKey");
  }

  public String getForm()
  {
    String form = "form";
    return getSelectedOption(form);
  }

  public String getContactStatus()
  {
    return getSelectedOption("contactStatus");
  }

  public String getAddressStatus()
  {
    return getSelectedOption("addressStatus");
  }

}
