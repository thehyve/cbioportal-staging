/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

/**
 * Main ETL process.
 * 
 * @author pieter
 *
 */
@Component
public class ETLProcessRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

    @Autowired
    private Extractor extractor;
    
    @Autowired
    private Transformer transformer;
    
    @Autowired
    private Validator validator;
    
    @Autowired
    private Loader loader;
    
    @Autowired
    private Restarter restarter;
    
    /**
     * Runs all the steps of the ETL process.
     * 
     * @param indexFile: index YAML file containing the names of the files to be "ETLed".
     * 
     * @throws InterruptedException
     * @throws TemplateException 
     * @throws IOException 
     * @throws ConfigurationException 
     * @throws ParseException 
     * @throws MalformedTemplateNameException 
     * @throws TemplateNotFoundException 
     */
    public void run(Resource indexFile) throws InterruptedException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, ConfigurationException, IOException, TemplateException {

        try  {
            startProcess();
            //E (Extract) step:
            Pair<Integer, List<String>> idAndStudies = extractor.run(indexFile);
            //T (Transform) step:
            transformer.transform(idAndStudies.getKey(), idAndStudies.getValue(), "command");
            //V (Validate) step:
            ArrayList<String> validatedStudies = validator.validate(idAndStudies.getKey(), idAndStudies.getValue());
            //L (Load) step:
            loader.load(idAndStudies.getKey(), validatedStudies);
            
            //dummy code just to let it take some time...until we have real steps above:
            //try {
            //    TimeUnit.SECONDS.sleep(10);
            //} catch (InterruptedException e) {
                // TODO Auto-generated catch block
            //    e.printStackTrace();
            //}

        }
        finally
        {
            //restart cBioPortal:
            restarter.restart();
            
            //end process / release lock:
            endProcess();
        }
    }

    /**
     * Socket configuration and respective synchronized start method that ensures only one
     * ETL process runs at a time (also if started in another process - this is guaranteed by
     * implementation of a race condition on a single socket). This is important because
     * cBioPortal currently only supports loading one study at a time, since this data loading
     * operation is not thread safe in cBioPortal itself.
     */
    @Value("${etl.lock.port:9999}")
    private Integer PORT;
    private static ServerSocket socket;

    private synchronized void startProcess() {
        try {
            //"Lock implementation". Try to reserve a socket:
            logger.info("Reserving socket on port " + PORT + " to avoid parallel ETL processes (not supported)");

            socket = new ServerSocket(PORT,0,InetAddress.getByAddress(new byte[] {127,0,0,1}));
        }
        catch (BindException e) {
            logger.error("Another ETL process is already running.", e);
            throw new RuntimeException("Another ETL process is already running", e);
        }
        catch (IOException e) {
            logger.error("Unexpected error.", e);
            e.printStackTrace();
            throw new RuntimeException("Unexpected error.", e);
        }
    }

    private void endProcess() {
        try {
            logger.info("Closing socket / releasing lock" );
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
