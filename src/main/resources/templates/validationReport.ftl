Dear cBioPortal Administrator,<br><br>
New studies were found at <b>${scanLocation}</b>. They were transformed to cBioPortal staging files and 
the staging files have been validated. This is the validation status for each of the studies:
<#list studies as name, status>
  <#if status == "VALID">
    <p>- ${name}, status: <b><font style="color: #04B404">VALID</font></b>
  <#elseif status == "VALID with WARNINGS">
    <p>- ${name}, status: <b><font style="color: #FFBF00">VALID with WARNINGS</font></b>
  <#elseif status == "ERRORS">
    <p>- ${name}, status: <b><font style="color: #FF0000">ERRORS</font></b>
  </#if>
</#list>
<br><br>The validation reports and log files can be found here:
<#list files as name, url>
<p>- ${name}: <a href="${url}">${url}</a>
</#list>
<br><br>The system will proceed and attempt loading the 
<#if level == "ERROR">
<b><font style="color: #04B404">VALID</font></b> and <b><font style="color: #FFBF00">VALID with WARNINGS</font></b>
<#elseif level == "WARNING">
<b><font style="color: #04B404">VALID</font></b>
</#if> studies. 
Please update any other studies accordingly.<br><br>
Regards,<br>
cBioPortal staging app. </div>