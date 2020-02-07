Timestamp: ${timestamp?datetime}
Study files not found!!!
Missing files:
<#list failedStudies as studyId, failedFiles>
    STUDY: ${studyId}
    <#list failedFiles as file>
        - ${file}
    </#list>
</#list>
