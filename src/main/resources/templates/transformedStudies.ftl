<div>Dear cBioPortal Administrator,<br><br>
The system tried to transform the studies below. This is the validation status of the transformation process for each of the studies:
<#list studies as name, status>
  <#if status == "VALID">
    <p>- ${name}, status: <b><font style="color: #04B404">VALID</font></b>
  <#elseif status == "VALID with WARNINGS">
    <p>- ${name}, status: <b><font style="color: #FFBF00">VALID with WARNINGS</font></b>
  <#elseif status == "ERRORS">
    <p>- ${name}, status: <b><font style="color: #FF0000">ERRORS</font></b>
  </#if>
</#list>
<br><br> The log files can be found here:
<#list files as name, url>
<p>- ${name}: <a href="${url}">${url}</a>
</#list>
<br><br>The system will only proceed and attempt loading the 
<b><font style="color: #04B404">VALID</font></b> and <b><font style="color: #FFBF00">VALID with WARNINGS</font></b> studies.
If there are any studies with <b><font style="color: #FF0000">ERRORS</font></b>, 
you need to update the studies accordingly and restart the staging app to continue.<br><br>
Regards,<br>
cBioPortal staging app. </div>