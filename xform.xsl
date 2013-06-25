<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">

    <xsl:output indent="yes"/>

    <xsl:key name="distinct" match="//transition" use="concat(../../@name, @name, @target)"/>

    <xsl:template match="/">
        <bpmn2:definitions targetNamespace="http://www.omg.org/bpmn20">
            <bpmn2:process id="test" isExecutable="true">
                <xsl:apply-templates select="//state"/>
            </bpmn2:process>
        </bpmn2:definitions>
    </xsl:template>

    <xsl:template match="//state">
        <bpmn2:manualTask id="{@name}">
        </bpmn2:manualTask>

        <xsl:choose>
            <xsl:when test="count(.//transition) > 1">
                <bpmn2:exclusiveGateway id="{@name}#outbound" gatewayDirection="Diverging">
                    <xsl:for-each select=".//transition[generate-id()=generate-id(key('distinct', concat(../../@name, @name, @target)))]">
                        <bpmn2:outgoing><xsl:value-of select="@target"/></bpmn2:outgoing>
                    </xsl:for-each>
                </bpmn2:exclusiveGateway>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>