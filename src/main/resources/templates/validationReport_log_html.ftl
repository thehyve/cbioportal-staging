Timestamp: <span class="date">${timestamp?datetime}</span><br>
Study validation status:
<ul>
  <#list studies as name, status>
    <#if status == "SUCCES">
      <li>${name}, status: <span style="color: #04B404; font-weight: bold;">VALID</span></li>
    <#elseif status == "WARNING">
      <li>${name}, status: <span style="color: #FFBF00; font-weight: bold;">VALID with WARNINGS</span></li>
    <#elseif status == "ERROR">
      <li>${name}, status: <span style="color: #FF0000; font-weight: bold;">ERRORS</span></li>
    </#if>
  </#list>
</ul>
Report and log files:
<ul>
  <#list files as name, url>
    <li>${name}: <a href="${url}">${url}</a></li>
  </#list>
</ul>
<br>