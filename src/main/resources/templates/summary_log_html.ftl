<div>
    <span style="font-size: 150%; font-weight: bold;"><u>cBioPortal Data Loading Summary Report</u></span>
    <br>
    <b>Study:</b> ${studyId}<br>
    <#if studyVersion == "">
            <#else >
                <b>Version:</b> ${studyVersion}<br>
            </#if>
    <b>Server:</b> ${serverAlias}<br>
    <br>
    <table border="1" cellspacing="0" cellpadding="1">
        <tr class="tableHeader">
            <td style="text-align:center;"><b>Step</b></td>
            <td style="text-align:center;"><b>Status</b></td>
            <td style="text-align:center;"><b>Details</b></td>
        </tr>
        <tr class="tableBody">
            <td>Transform input files to cBioPortal staging files</td>
            <td><#if transformerStatus == "SUCCESS">
                <span style="color: #04B404; font-weight: bold;">VALID</span>
            <#elseif transformerStatus == "WARNING">
                <span style="color: #FFBF00; font-weight: bold;">VALID with WARNINGS</span>
            <#elseif transformerStatus == "ERROR">
                <span style="color: #FF0000; font-weight: bold;">ERRORS</span>
            <#elseif transformerStatus == "SKIPPED">
                <span style="color: #0000FF; font-weight: bold;">SKIPPED, INPUT FILES ARE ALREADY SUITABLE TO LOAD TO CBIOPORTAL</span>
            <#elseif transformerStatus == "null">
                <span style="color: #B0B0B0; font-weight: bold;">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</span>
            </#if></td>
            <td><#if transformerLog == "">
            <#else >
                <a href="${transformerLog}">Transformation log</a></li>
            </#if></td>
        </tr>
        <tr class="tableBody">
            <td>Validation of cBioPortal staging files</td>
            <td><#if validatorStatus == "SUCCESS">
                <span style="color: #04B404; font-weight: bold;">VALID</span>
            <#elseif validatorStatus == "WARNING">
                <span style="color: #FFBF00; font-weight: bold;">VALID with WARNINGS</span>
            <#elseif validatorStatus == "ERROR">
                <span style="color: #FF0000; font-weight: bold;">ERRORS</span>
            <#elseif validatorStatus == "null">
                <span style="color: #B0B0B0; font-weight: bold;">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</span>
            </#if></td>
            <td><#if validatorLog == "">
            <#else >
                <a href="${validatorReport}">Validation report</a></li>
                <br>
                <a href="${validatorLog}">Validation log</a></li>
            </#if></td>
        </tr>
        <tr class="tableBody">
            <td>Loading of cBioPortal staging files</td>
            <td><#if loaderStatus == "SUCCESS">
                <span style="color: #04B404; font-weight: bold;">SUCCESS</span>
            <#elseif loaderStatus == "ERROR">
                <span style="color: #FF0000; font-weight: bold;">ERRORS</span>
            <#elseif loaderStatus == "null">
                <span style="color: #B0B0B0; font-weight: bold;">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</span>
            </#if></td>
            <td><#if loaderLog == "">
            <#else >
                <a href="${loaderLog}">Loading log</a></li>
            </#if></td>
        </tr>
        <tr class="tableBody">
            <td><b>Summary</b></td>
            <td><#if summaryStatus == "SUCCESS">
                <span style="color: #04B404; font-weight: bold;">SUCCESS</span>
            <#elseif summaryStatus == "ERROR">
                <span style="color: #FF0000; font-weight: bold;">ERRORS</span>
            </#if></td>
            <td><#if summaryStatus == "SUCCESS">
                The study has been loaded in cBioPortal.
            <#elseif summaryStatus == "ERROR">
                Please, check the logs and report files for more details on what went wrong.
            </#if></td>
        </tr>
    </table>
</div>