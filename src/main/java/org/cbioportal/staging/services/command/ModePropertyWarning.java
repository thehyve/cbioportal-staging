/*
* Copyright (c) 2018 The Hyve B.V.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.cbioportal.staging.services.command;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.RestarterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/*
    This class provides an exception when the the required
    'cbioportal.mode' param is not defined. Other beans that
    depend on it are created conditionally and cannot provide
    a detailed error message when the param is missing.
*/
@Component
public class ModePropertyWarning implements IRestarter, ICommandBuilder {

    @Value("${cbioportal.mode:}")
    private String runMode;

    @PostConstruct
    public void checkParam() throws ConfigurationException {
        if (runMode.equals("")) {
            throw new ConfigurationException("The 'cbioportal.mode' property is not set. Please check the application properties file.");
        }
    }

    public void restart() throws RestarterException {
        throw new RestarterException("Unimplemented method. Set the cbioportal.mode property.");
    }

    @Override
    public ProcessBuilder buildPortalInfoCommand(Resource PortalInfoFolder) throws CommandBuilderException {
        throw new CommandBuilderException("Unimplemented method. Set the cbioportal.mode property.");
    }

    @Override
    public ProcessBuilder buildValidatorCommand(Resource studyPath, Resource portalInfoFolder, Resource reportFile)
    throws CommandBuilderException {
        throw new CommandBuilderException("Unimplemented method. Set the cbioportal.mode property.");
    }

    @Override
    public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
        throw new CommandBuilderException("Unimplemented method. Set the cbioportal.mode property.");
    }
}
