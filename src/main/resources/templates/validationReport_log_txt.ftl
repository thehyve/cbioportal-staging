Timestamp: ${timestamp?datetime}
Study validation status:
<#list studies as name, status>
  <#if status == "SUCCES">
    - ${name}, status: VALID
  <#elseif status == "WARNING">
    - ${name}, status: VALID with WARNINGS
  <#elseif status == "ERROR">
    - ${name}, status: ERRORS
  </#if>
</#list>
Report and log files:
<#list files as name, url>
  - ${name}: ${url}
</#list>