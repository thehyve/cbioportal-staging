package org.cbioportal.staging.services.etl;

import org.cbioportal.staging.exceptions.EtlUtilsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EtlUtils {

    @Value("${transformation.skip:false}")
    private boolean transformationSkip;

    @Value("${transformation.command.script:}")
    private String transformationCommandScript;

    @Value("${transformation.command.script.docker.image:}")
    private String transformationCommandScriptDockerImage;

    public boolean doTransformation() throws EtlUtilsException {
        if (transformationSkip) {
            if (!transformationCommandScript.equals("") && !transformationCommandScriptDockerImage.equals("")) {
                throw new EtlUtilsException("transformation.skip is true, but transformation.command.script and "+
                "transformation.command.script.docker.image are defined.");
            } else if (!transformationCommandScript.equals("")) {
                throw new EtlUtilsException("transformation.skip is true, but transformation.command.script is defined.");
            } else if (!transformationCommandScriptDockerImage.equals("")) {
                throw new EtlUtilsException("transformation.skip is true, but transformation.command.script.docker.image is defined.");
            } else {
                return false;
            }
        } else {
            if (transformationCommandScript.equals("") && transformationCommandScriptDockerImage.equals("")) {
                throw new EtlUtilsException("transformation.skip is false, but transformation.command.script and "+
                "transformation.command.script.docker.image are not defined. At least one of these two parameters need to be defined.");
            }
            return true;
        }
    }
}