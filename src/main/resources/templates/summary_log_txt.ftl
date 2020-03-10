--------------------------------------------------
///// cBioPortal Data Loading Summary Report /////
--------------------------------------------------
Study: ${studyId}
<#if studyVersion == "">
<#else >
Version: ${studyVersion}
</#if>
Server: ${serverAlias}

Steps:
- Transform input files to cBioPortal staging files: <#if transformerStatus == "SUCCESS">VALID<#elseif transformerStatus == "WARNING">VALID with WARNINGS<#elseif transformerStatus == "ERROR">ERRORS<#elseif transformerStatus == "SKIPPED">SKIPPED, INPUT FILES ARE ALREADY SUITABLE TO LOAD TO CBIOPORTAL<#elseif transformerStatus == "null">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</#if><#if transformerLog == ""> <#else > (Transformation log: ${transformerLog}).</#if>
- Validation of cBioPortal staging files: <#if validatorStatus == "SUCCESS">VALID<#elseif validatorStatus == "WARNING">VALID with WARNINGS<#elseif validatorStatus == "ERROR">ERRORS<#elseif validatorStatus == "null">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</#if><#if validatorLog == ""> <#else > (Validation report and log: ${validatorReport}, ${validatorLog}).</#if>
- Loading of cBioPortal staging files: <#if loaderStatus == "SUCCESS">SUCCESS<#elseif loaderStatus == "ERROR">ERRORS<#elseif loaderStatus == "null">NOT DONE DUE TO ERRORS IN PREVIOUS STEPS</#if><#if loaderLog == ""> <#else > (Loading log: ${loaderLog}).</#if>
- Summary: <#if summaryStatus == "SUCCESS">SUCCESS. <#elseif summaryStatus == "ERROR">ERRORS. </#if><#if summaryStatus == "SUCCESS">The study has been loaded in cBioPortal. <#elseif summaryStatus == "ERROR">Please, check the logs and report files for more details on what went wrong.</#if>
--------------------------------------------------
