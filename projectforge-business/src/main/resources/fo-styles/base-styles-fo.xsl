<?xml version='1.0' encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" version='1.0'>
  <!-- This files contains the xslt transformation for rendering html like files to xsl-fo. -->
  <!-- Here you can define central styles reused in all templates. -->

  <!-- Central font definitions -->
  <xsl:template name="base-font-family">
    <xsl:attribute name="font-family"><xsl:text>Helvetica</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="bold-font-family">
    <xsl:attribute name="font-family"><xsl:text>Helvetica</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="font-weight"><xsl:text>bold</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-tiny">
    <xsl:call-template name="base-font-family" />
    <xsl:attribute name="font-size"><xsl:text>6pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>8pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-small">
    <xsl:call-template name="base-font-family" />
    <xsl:attribute name="font-size"><xsl:text>8pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>10pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-normal">
    <xsl:call-template name="base-font-family" />
    <xsl:attribute name="font-size"><xsl:text>10pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>14pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-normal-bold">
    <xsl:call-template name="bold-font-family" />
    <xsl:attribute name="font-size"><xsl:text>10pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>14pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-large">
    <xsl:call-template name="base-font-family" />
    <xsl:attribute name="font-size"><xsl:text>12pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>16pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="font-large-bold">
    <xsl:call-template name="bold-font-family" />
    <xsl:attribute name="font-size"><xsl:text>12pt</xsl:text>
    </xsl:attribute>
    <xsl:attribute name="line-height"><xsl:text>16pt</xsl:text>
    </xsl:attribute>
  </xsl:template>

  <!-- Hier können verschiedene Page-Master definiert werden. -->
  <xsl:template name="include-page-landscape-master-standard">
    <fo:simple-page-master master-name="standard" page-height="21cm" page-width="29.7cm" margin-top="1cm" margin-bottom="2cm" margin-left="2.5cm"
      margin-right="2.5cm">
      <fo:region-body margin-top="12mm" margin-bottom="1.5cm" />
      <fo:region-before extent="3cm" />
      <fo:region-after extent="1.5cm" />
    </fo:simple-page-master>
  </xsl:template>

  <xsl:template match="space">
    <xsl:value-of select="' '" />
  </xsl:template>

  <xsl:template match="workaround-space">
    <!-- Kann bei neuem fop eleganter gelöst werden (unsichtbarer Text). -->
    <fo:inline color="white">
      <xsl:apply-templates />
    </fo:inline>
  </xsl:template>

  <xsl:template match="br">
    <fo:block />
  </xsl:template>

  <xsl:template match="h1-color">
    <fo:block background-color="#0056A7" color="white" font-size="16pt" line-height="20pt" padding="5pt" border-width="0pt"
      space-after.optimum="15pt">
      <xsl:copy-of select="@*" />
      <xsl:call-template name="bold-font-family" />
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="h1">
    <fo:block font-size="16pt" line-height="20pt" space-after.optimum="15pt" padding-top="15pt">
      <xsl:copy-of select="@*" />
      <xsl:call-template name="bold-font-family" />
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="h2">
    <fo:block font-size="14pt" line-height="16pt" space-after.optimum="10pt" padding-top="10pt">
      <xsl:copy-of select="@*" />
      <xsl:call-template name="bold-font-family" />
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="h3">
    <fo:block font-size="12pt" line-height="14pt" space-after.optimum="5pt" padding-top="5pt">
      <xsl:copy-of select="@*" />
      <xsl:call-template name="bold-font-family" />
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="block">
    <fo:block>
      <xsl:call-template name="copy-and-set-text-attributes" />
      <xsl:if test="not(@padding-bottom)">
        <xsl:attribute name="padding-bottom"><xsl:text>10pt</xsl:text>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="not(@padding-top)">
        <xsl:attribute name="padding-top"><xsl:text>10pt</xsl:text>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="span">
    <fo:inline>
      <xsl:call-template name="copy-and-set-text-attributes" />
      <xsl:apply-templates />
    </fo:inline>
  </xsl:template>

  <xsl:template match="hr">
    <fo:block margin-bottom="5pt">
      <fo:leader leader-length="100%" rule-thickness="0.5pt" leader-pattern="rule" />
    </fo:block>
  </xsl:template>

  <xsl:template name="copy-and-set-text-attributes">
    <xsl:copy-of select="@*[not(name()='use-font') and not(name()='border-top-line') and not(name()='border-bottom-line')]" />
    <xsl:call-template name="set-text-attributes" />
  </xsl:template>

  <xsl:template name="set-text-attributes">
    <xsl:choose>
      <xsl:when test="@use-font = 'small'">
        <xsl:call-template name="font-small" />
      </xsl:when>
      <xsl:when test="@use-font = 'bold'">
        <xsl:call-template name="bold-font-family" />
      </xsl:when>
      <xsl:when test="@use-font = 'tiny'">
        <xsl:call-template name="font-tiny" />
      </xsl:when>
      <xsl:when test="@use-font = 'large'">
        <xsl:call-template name="font-large" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="ol|ul">
    <fo:list-block start-indent="1cm" provisional-distance-between-starts="12pt" provisional-label-separation="0.15cm">
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </fo:list-block>
  </xsl:template>

  <xsl:template match="li">
    <fo:list-item>
      <xsl:choose>
        <xsl:when test="position() = 1">
          <xsl:attribute name="space-before">
            <xsl:value-of select="'1em'" />
          </xsl:attribute>
          <xsl:attribute name="space-after">
            <xsl:value-of select="'0.5em'" />
          </xsl:attribute>
        </xsl:when>
        <xsl:when test="position() = last()">
          <xsl:attribute name="space-after">
            <xsl:value-of select="'1em'" />
          </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="space-after">
            <xsl:value-of select="'0.5em'" />
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <fo:list-item-label end-indent="label-end()">
        <xsl:choose>
          <xsl:when test="parent::ol">
            <!-- Ordered list. -->
            <fo:block>
              <xsl:value-of select="concat(position(), '.')" />
            </fo:block>
            <!-- TODO: Nested lists. -->
          </xsl:when>
          <xsl:otherwise>
            <!-- Unordered list. -->
            <fo:block>&#x2022;</fo:block>
            <!-- TODO: Nested lists. -->
          </xsl:otherwise>
        </xsl:choose>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start() + 1em">
        <xsl:choose>
          <xsl:when test="not(child::block)">
            <fo:block>
              <xsl:call-template name="font-normal" />
              <xsl:apply-templates />
            </fo:block>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates />
          </xsl:otherwise>
        </xsl:choose>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <xsl:template match="table">
    <fo:table table-layout="fixed">
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </fo:table>
  </xsl:template>

  <xsl:template match="table-column">
    <fo:table-column>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </fo:table-column>
  </xsl:template>

  <xsl:template match="table-body">
    <fo:table-body>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </fo:table-body>
  </xsl:template>

  <xsl:template match="table-header">
    <fo:table-header>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </fo:table-header>
  </xsl:template>

  <xsl:template match="tr">
    <fo:table-row height="16pt">
      <xsl:copy-of
        select="@*[not(name()='border-top-line') and not(name()='border-bottom-line') and not(name()='use-font') and not(name()='even-odd')]" />
      <xsl:call-template name="set-text-attributes" />
      <xsl:if test="@even-odd and position() mod 2 != 0">
        <xsl:attribute name="background-color">rgb(234,234,234)</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates />
    </fo:table-row>
  </xsl:template>

  <xsl:template name="create-row-border">
    <xsl:if test="parent::tr/@border-top-line = 'true' or @border-top-line = 'true'">
      <!-- Leider ist bei Fop 0.20.5 noch keine zeilenweise Border unterstützt. -->
      <xsl:attribute name="border-top-width"><xsl:text>1pt</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="border-top-style"><xsl:text>solid</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="border-top-color"><xsl:text>black</xsl:text>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="parent::tr/@border-bottom-line = 'true' or @border-bottom-line = 'true'">
      <!-- Leider ist bei Fop 0.20.5 noch keine zeilenweise Border unterstützt. -->
      <xsl:attribute name="border-bottom-width"><xsl:text>1pt</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-style"><xsl:text>solid</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-color"><xsl:text>black</xsl:text>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="td">
    <fo:table-cell>
      <xsl:call-template name="copy-and-set-text-attributes" />
      <xsl:call-template name="create-row-border" />
      <xsl:choose>
        <xsl:when test="not(child::block)">
          <fo:block start-indent="2pt" end-indent="2pt">
            <xsl:call-template name="font-normal" />
            <xsl:apply-templates />
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates />
        </xsl:otherwise>
      </xsl:choose>
    </fo:table-cell>
  </xsl:template>

  <xsl:template match="th">
    <fo:table-cell>
      <xsl:call-template name="copy-and-set-text-attributes" />
      <xsl:call-template name="create-row-border" />
      <xsl:choose>
        <xsl:when test="not(child::block)">
          <fo:block start-indent="2pt" end-indent="2pt">
            <xsl:call-template name="font-normal" />
            <xsl:call-template name="bold-font-family" />
            <xsl:apply-templates />
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates />
        </xsl:otherwise>
      </xsl:choose>
    </fo:table-cell>
  </xsl:template>

  <xsl:template match="image">
    <fo:external-graphic>
      <xsl:copy-of select="@*[not(name()='src')]" />
      <xsl:attribute name="src">
        <xsl:choose>
          <xsl:when test="starts-with(@src, '/')">
            <xsl:value-of select="@src" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($baseDir, '/', @src)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </fo:external-graphic>
  </xsl:template>

  <xsl:template name="pdf-logo">
    <xsl:param name="height" select="'20mm'" />
    <fo:external-graphic scaling="uniform">
      <xsl:attribute name="height">
        <xsl:value-of select="$height" />
      </xsl:attribute>
      <xsl:attribute name="src">
        <xsl:value-of select="$logoFile" />
      </xsl:attribute>
    </fo:external-graphic>
  </xsl:template>


  <xsl:template name="standard-header">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xlink="http://www.w3.org/1999/xlink">
      <fo:layout-master-set>
        <xsl:call-template name="include-page-landscape-master-standard" />
      </fo:layout-master-set>

      <fo:page-sequence master-reference="standard">
        <!-- header -->
        <fo:static-content flow-name="xsl-region-before">
          <fo:block>
            <fo:table table-layout="fixed" width="100%">
              <fo:table-column column-width="proportional-column-width(1)" />
              <fo:table-column column-width="40mm" />
              <fo:table-body>
                <fo:table-row height="20mm">
                  <fo:table-cell display-align="before">
                    <fo:block text-align="start" font-size="14pt" color="#999999">
                      <xsl:call-template name="bold-font-family" />
                      <xsl:value-of select="$organization" />
                    </fo:block>
                  </fo:table-cell>
                  <fo:table-cell display-align="before">
                    <fo:block text-align="end">
                      <xsl:call-template name="pdf-logo" />
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
              </fo:table-body>
            </fo:table>
          </fo:block>
        </fo:static-content>
        <fo:static-content flow-name="xsl-region-after">
          <fo:block space-before.optimum="8pt" line-height="12pt">
            <xsl:call-template name="font-small" />
            <fo:table table-layout="fixed" width="100%">
              <fo:table-column column-width="proportional-column-width(1)" />
              <fo:table-column column-width="20mm" />
              <fo:table-column column-width="proportional-column-width(1)" />
              <fo:table-body>
                <fo:table-row height="1pt">
                  <fo:table-cell number-columns-spanned="3">
                    <fo:block>
                      <fo:leader leader-length="100%" rule-thickness="0.5pt" leader-pattern="rule" />
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="12pt">
                  <fo:table-cell display-align="center">
                    <fo:block text-align="start">
                      www.projectforge.org | Version
                      <xsl:value-of select="$appVersion" />
                    </fo:block>
                  </fo:table-cell>
                  <fo:table-cell display-align="center">
                    <fo:block text-align="center">
                      <fo:page-number />
                      /
                      <fo:page-number-citation ref-id="lastpage" />
                    </fo:block>
                  </fo:table-cell>
                  <fo:table-cell display-align="center">
                    <fo:block text-align="end">
                      <xsl:value-of select="concat(/template-fo/@createdLabel, ': ', /template-fo/@systemDate, ', ', /template-fo/@loggedInUser)" />
                    </fo:block>
                  </fo:table-cell>
                </fo:table-row>
              </fo:table-body>
            </fo:table>
          </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">
          <xsl:apply-templates />
          <fo:block id="lastpage"></fo:block>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>

</xsl:stylesheet>