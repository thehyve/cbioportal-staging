Timestamp: <span class="date">${timestamp?datetime}</span><br>
<span style="color: #FF0000; font-weight: bold;">Study files not found!!!</span><br>
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