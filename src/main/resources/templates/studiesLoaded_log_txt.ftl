Timestamp: ${timestamp?datetime}
Study loading status:
<#list studies as name, status>
  <#if status == "SUCCESS">
    - ${name}, status: VALID
  <#elseif status == "ERROR">
    - ${name}, status: ERRORS
  </#if>
</#list>
Log files:
<#list files as name, url>
  - ${name}: ${url}
</#list>
