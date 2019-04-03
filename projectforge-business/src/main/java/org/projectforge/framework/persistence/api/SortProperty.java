package org.projectforge.framework.persistence.api;

public class SortProperty {
  private SortOrder sortOrder;
  private String property;

  public SortProperty() {
  }

  public SortProperty(String property) {
    this(property, SortOrder.ASCENDING);
  }

  public SortProperty(String property, SortOrder sortOrder) {
    this.property = property;
    this.sortOrder = sortOrder;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }
}
