package org.projectforge.business.fibu;

import org.projectforge.common.i18n.I18nEnum;

public enum AuftragFakturiertFilterStatus implements I18nEnum
{
  ALL("all"),
  FAKTURIERT("vollstaendigFakturiert"),
  NICHT_FAKTURIERT("nochNichtVollstaendigFakturiert");

  private final String i18nKey;

  AuftragFakturiertFilterStatus(final String i18nKey)
  {
    this.i18nKey = i18nKey;
  }

  @Override
  public String getI18nKey()
  {
    return "fibu.auftrag.filter.type." + i18nKey;
  }
}
