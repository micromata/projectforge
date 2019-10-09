<?xml version='1.0' encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version='1.0'>
  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
  <xsl:template match="/root">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <xsl:copy-of select="*"/>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>