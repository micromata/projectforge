<?xml version='1.0' encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>
  <!-- This script is used for rendering timesheets to xsl-fo. -->
  
  <xsl:import href="base-styles-fo.xsl" />
  
  <xsl:output method="xml" indent="no" encoding="UTF-8"/>

  <xsl:param name="baseDir" select="''" />
  <xsl:param name="logoFile" select="''" />
  <xsl:param name="appId" select="''" />
  <xsl:param name="appVersion" select="''" />
  <xsl:param name="organization" select="''" />
  
  <xsl:template match="/template-fo">
    <xsl:call-template name="standard-header" />
  </xsl:template>
</xsl:stylesheet>