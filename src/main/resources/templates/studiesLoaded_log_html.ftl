Timestamp: <span class="date">${timestamp?datetime}</span><br>
Study loading status:<br>
<ul>
  <#list studies as name, status>
    <#if status == "SUCCESS">
      <li>${name}, status: <span class="success">VALID</span></li>
    <#elseif status == "ERROR">
      <li>${name}, status: <span class="error">ERRORS</span></li>
    </#if>
  </#list>
</ul><br>
<br>
Log files:<br>
<ul>
  <#list files as name, url>
    <li>${name}: <a href="${url}">${url}</a></li>
  </#list>
</ul><br>
<br>
