package com.netgrif.mongo.quartz;

import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.netgrif.quartz.mongodb.Constants;
import com.netgrif.quartz.mongodb.LockManager;
import com.netgrif.quartz.mongodb.dao.LocksDao;
import com.netgrif.quartz.mongodb.util.ExpiryCalculator;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerKey;

import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LockManagerTest {

    @Mock
    private LocksDao locksDao;

    @Mock
    private ExpiryCalculator expiryCalc;

    @InjectMocks
    private LockManager manager;

    private TriggerKey tkey;

    @BeforeEach
    public void setup() {
        tkey = new TriggerKey("n1", "g1");
    }

    private MongoWriteException newWriteException() {
        return new MongoWriteException(
                new WriteError(1, "Rly!", BsonDocument.parse("{}")),
                new ServerAddress());
    }

    @Test
    public void tryLockShouldLockTriggerWhenHaveNoLocks() {
        manager.tryLock(tkey);
        verify(locksDao).lockTrigger(tkey);
    }

    @Test
    public void tryLockCannotGetExistingLockForExpirationCheck() {
        doThrow(newWriteException()).when(locksDao).lockTrigger(tkey);

        assertFalse(manager.tryLock(tkey));
    }

    @Test
    public void shouldNotRelockWhenLockIsNotFound() {
        when(locksDao.findTriggerLock(tkey)).thenReturn(null);

        assertFalse(manager.relockExpired(tkey));
    }

    @Test
    public void shouldNotRelockValidLock() {
        Document existingLock = new Document();
        when(locksDao.findTriggerLock(tkey)).thenReturn(existingLock);
        when(expiryCalc.isTriggerLockExpired(existingLock)).thenReturn(false);

        assertFalse(manager.relockExpired(tkey));
    }

    @Test
    @Disabled("Dorobit...")
    public void shouldRelockExpiredLock() {
        Date lockTime = new Date(123);
        Document existingLock = new Document(Constants.LOCK_TIME, lockTime);
        boolean relocked = manager.relockExpired(tkey);

        assert manager.relockExpired(tkey);
    }


}
