<div>
    Dear cBioPortal Administrator,
    <br>
    <br>
    The system tried to transform the studies below. This is the validation status of the transformation process for each of the studies:
    <ul>
        <#list studies as name, status>
        <#if status == "SUCCESS">
            <li>${name}, status: <span style="color: #04B404; font-weight: bold;">VALID</span></li>
        <#elseif status == "WARNING">
            <li>${name}, status: <span style="color: navy; font-weight: bold;">VALID with WARNINGS</span></li>
        <#elseif status == "ERROR">
            <li>${name}, status: <span style="color: #FF0000; font-weight: bold;">ERRORS</span></li>
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
    <br>
    The system will only proceed and attempt loading the <span style="color: #04B404; font-weight: bold;">VALID</span>
    and <span style="color: navy; font-weight: bold;">VALID with WARNINGS</span> studies.
    If there are any studies with <span style="color: #FF0000; font-weight: bold;">ERRORS</span>,
    you need to update the studies accordingly and restart the staging app to continue.
    <br>
    <br>
    Regards,
    <br>
    cBioPortal staging app.
</div>