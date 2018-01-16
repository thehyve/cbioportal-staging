package org.cbioportal.staging.etl;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

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
    Extractor extractor;
    
    /**
     * Runs all the steps of the ETL process.
     * 
     * @param indexFile: index YAML file containing the names of the files to be "ETLed".
     * 
     * @throws InterruptedException
     */
    public void run(Resource indexFile) throws InterruptedException {

        try  {
            startProcess();
            //E (Extract) step:
            extractor.run(indexFile);
            //T (Transform) step:
            //TODO transformer
            //L (Load) step
            //TODO loader
            
        }
        finally
        {
            //restart cBioPortal:
            //TODO
            
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
            logger.error("ETL process already running.", e);
            System.exit(1);
        }
        catch (IOException e) {
            logger.error("Unexpected error.", e);
            e.printStackTrace();
            System.exit(2);
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
