Timestamp: ${timestamp?datetime}
Study transformation status:
<#list studies as name, status>
  <#if status == "SUCCESS">
    - ${name}, status: VALID
  <#elseif status == "WARNING">
    - ${name}, status: VALID with WARNINGS
  <#elseif status == "ERROR">
    - ${name}, status: ERRORS
  </#if>
</#list>
Log files:
<#list files as name, url>
  - ${name}: ${url}
</#list>
