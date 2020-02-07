<head>
    <#include "style.css">
</head>
<body>
    <div>
        Dear cBioPortal Administrator,
        <br>
        <br>
        The system tried to transform the studies below. This is the validation status of the transformation process for each of the studies:
        <ul>
            <#list studies as name, status>
            <#if status == "SUCCESS">
                <li>${name}, status: <span class="success">VALID</span></li>
            <#elseif status == "WARNING">
                <li>${name}, status: <span class="warning">VALID with WARNINGS</span></li>
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
        <br>
        The system will only proceed and attempt loading the <span class="success">VALID</span>
        and <span class="warning">VALID with WARNINGS</span> studies.
        If there are any studies with <b class="error">ERRORS</span>,
        you need to update the studies accordingly and restart the staging app to continue.
        <br>
        <br>
        Regards,
        <br>
        cBioPortal staging app.
    </div>
</body>