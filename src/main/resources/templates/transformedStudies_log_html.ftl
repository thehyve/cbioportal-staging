Timestamp: <span class="date">${timestamp?datetime}</span><br>
Study transformation status:<br>
<ul>
  <#list studies as name, status>
    <#if status == "SUCCESS">
      <li>${name}, status: <span style="color: #04B404; font-weight: bold;">VALID</span></li>
    <#elseif status == "WARNING">
      <li>${name}, status: <span style="color: #FFBF00; font-weight: bold;">VALID with WARNINGS</span></li>
    <#elseif status == "ERROR">
      <li>${name}, status: <span style="color: #FF0000; font-weight: bold;">ERRORS</span></li>
    </#if>
  </#list>
</ul><br>
<br>Log files:<br>
<ul>
  <#list files as name, url>
    <li>${name}: <a href="${url}">${url}</a></li>
  </#list>
</ul><br>
<br>