<head>
  <#include "style.css">
</head>
<body>
  <div>
    Dear cBioPortal Administrator,
    <br>
    <br>
    The system tried loading the studies below. This is the loading status for each of the studies:
    <ul>
      <#list studies as name, status>
        <#if status == "SUCCESS">
          <li>${name}, status: <span class="success">VALID</span></li>
        <#elseif status == "ERROR">
          <li>${name}, status: <span class="error">ERRORS</span></li>
        </#if>
      </#list>
    </ul>
    <br>
    <br>
    The log files can be found here:
    <ul>
      <#list files as name, url>
        <li>${name}: <a href="${url}">${url}</a></li>
      </#list>
    </ul>
    <br>
    <br>The <span class="success">SUCCESSFULLY LOADED</span> studies are
    available for querying in the portal.
    <br>
    <br>
    Regards,
    <br>
    cBioPortal staging app.
  </div>
</body>