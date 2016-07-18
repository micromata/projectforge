package org.projectforge.web.selenium.fibu;

import org.projectforge.web.selenium.EditPage;
import org.projectforge.web.selenium.TestPageBase;

/**
 * TODO give this class a new name.
 * Its purpose is to hold all datafields that MyAccountEditPage and EmployeeEditPage share.
 * @param <S>
 */
public abstract class AddressAndBankingEditingPage<S extends TestPageBase> extends EditPage<S>
{
  public AddressAndBankingEditingPage() {
    super();
  }

  public AddressAndBankingEditingPage(int id) {
    super(id);
  }
  /**
   * @param accountHolder
   */
  public S setAccountHolder(String accountHolder)
  {
    return setStringElementById("accountHolder", accountHolder);
  }

  public String getAccountHolder()
  {
    return getStringElementById("accountHolder");
  }

  /**
   * @param iban
   */
  public S setIban(String iban)
  {
    return setStringElementById("iban", iban);
  }

  public String getIban()
  {
    return getStringElementById("iban");
  }

  /**
   * @param bic
   */
  public S setBic(String bic)
  {
    return setStringElementById("bic", bic);
  }

  public String getBic()
  {
    return getStringElementById("bic");
  }

  public S setStreet(String street)
  {
    return setStringElementById("street", street);
  }

  public String getStreet()
  {
    return getStringElementById("street");
  }

  public S setState(String state)
  {
    return setStringElementById("state", state);
  }

  public String getState()
  {
    return getStringElementById("state");
  }

  public S setCountry(String country)
  {
    return setStringElementById("country", country);
  }

  public String getCountry()
  {
    return getStringElementById("country");
  }

  public S setZipCode(String zipcode)
  {
    return setStringElementById("zipCode", zipcode);
  }

  public String getZipCode()
  {
    return getStringElementById("zipCode");
  }

  public S setCity(String city)
  {
    return setStringElementById("city", city);
  }

  public String getCity()
  {
    return getStringElementById("city");
  }

  public String getBirthday() {
    return getStringElementById("birthday");
  }

  public S setBirthday(String birthday) {
    return  setStringElementById("birthday", birthday);
  }

}
