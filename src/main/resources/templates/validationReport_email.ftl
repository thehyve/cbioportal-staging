<div>
    Dear cBioPortal Administrator,
    <br>
    <br>
    New studies were found at <b>${scanLocation}</b>. They were transformed to cBioPortal staging files and
    the staging files have been validated. This is the validation status for each of the studies:
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
    <br>
    <br>The validation reports and log files can be found here:
    <ul>
        <#list files as name, url>
            <li>${name}: <a href="${url}">${url}</a></li>
        </#list>
    </ul>
    <br>
    <br>The system will proceed and attempt loading the
    <#if level == "ERROR">
        <span style="color: #04B404; font-weight: bold;">VALID</span> and <span style="color: #FFBF00; font-weight: bold;">VALID with WARNINGS</span>
    <#elseif level == "WARNING">
        <span style="color: #04B404; font-weight: bold;">VALID</span>
    </#if> studies.
    Please update any other studies accordingly.
    <br>
    <br>
    Regards,
    <br>
    cBioPortal staging app.
</div>