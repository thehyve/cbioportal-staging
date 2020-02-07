<head>
    <#include "style.css">
</head>
<body>
    <div>
        Dear cBioPortal Administrator,
        <br>
        <br>
        While checking the scan location location for the study files, the following files were found to be missing (after trying 5 times over a period of ${totalTime} minutes):
        <br>
        <#list failedStudies as studyId, failedFiles>
            STUDY: ${studyId}
            <ul>
                <#list failedFiles as file>
                    <li>${file}</li>
                </#list>
            </ul>
        </#list>
        <br>Please add these files or update the "list_of_studies.yaml" configuration file on S3.
        <br>
        <br>
        Regards,
        <br>
        cBioPortal staging app.
    </div>
</body>