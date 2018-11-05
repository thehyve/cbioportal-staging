<div>Dear cBioPortal Administrator,<br><br>
The system tried loading the studies below. This is the loading status for each of the studies:
<#list studies as name, status>
  <#if status == "SUCCESSFULLY LOADED">
    <p>- ${name}, status: <b><font style="color: #04B404">SUCCESSFULLY LOADED</font></b>
  <#elseif status == "ERRORS">
    <p>- ${name}, status: <b><font style="color: #FF0000">ERRORS</font></b>
  </#if>
</#list>
<br><br> The log files can be found here:
<#list files as name, url>
<p>- ${name}: <a href="${url}">${url}</a>
</#list>
<br><br>The <b><font style="color: #04B404">SUCCESSFULLY LOADED</font></b> studies are 
available for querying in the portal.<br><br>
Regards,<br>
cBioPortal staging app. </div>