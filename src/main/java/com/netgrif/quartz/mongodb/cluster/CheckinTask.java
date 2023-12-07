    package com.netgrif.quartz.mongodb.cluster;

import com.mongodb.MongoWriteConcernException;
import com.netgrif.quartz.mongodb.dao.SchedulerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckinTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CheckinTask.class);

    private SchedulerDao schedulerDao;
    private Runnable errorHandler;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 1000;

    public CheckinTask(SchedulerDao schedulerDao, Runnable errorHandler) {
        this.schedulerDao = schedulerDao;
        this.errorHandler = errorHandler;
    }

    public void setErrorHandler(Runnable errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try {
            schedulerDao.checkIn();
            retryCount = 0;
        } catch (MongoWriteConcernException e) {
            handleWriteConcernException(e);
        } catch (Exception e) {
            log.error("Unexpected error during check-in: " + e.getMessage(), e);
            //errorHandler.run();  //TODO: ups..
        }
    }

    private void handleWriteConcernException(MongoWriteConcernException e) {
        if (retryCount < MAX_RETRIES) {
            log.warn("WriteConcernException occurred, retrying check-in... Attempt: " + (retryCount + 1));
            try {
                Thread.sleep(RETRY_DELAY_MS * retryCount);
                retryCount++;
                run();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted during retry delay", ie);
            } catch (Exception ee) {
                log.error("Hups..." +ee.getMessage() , ee);
            }
        } else {
            log.error("Maximum number of attempts reached. Unable to complete check-in.", e);
            errorHandler.run();
        }
    }
}