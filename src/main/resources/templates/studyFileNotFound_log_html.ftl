Timestamp: <span class="date">${timestamp?datetime}</span><br>
<span class="error">Study files not found!!!</span><br>
Missing files:<br>
<#list failedStudies as studyId, failedFiles>
    STUDY: ${studyId}
    <ul>
        <#list failedFiles as file>
            <li>${file}</li>
        </#list>
    </ul>
</#list><br>
<br>