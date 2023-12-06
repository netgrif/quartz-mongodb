package com.netgrif.mongo.quartz;


import com.netgrif.quartz.mongodb.Constants;
import com.netgrif.quartz.mongodb.TriggerStateManager;
import com.netgrif.quartz.mongodb.dao.JobDao;
import com.netgrif.quartz.mongodb.dao.PausedJobGroupsDao;
import com.netgrif.quartz.mongodb.dao.PausedTriggerGroupsDao;
import com.netgrif.quartz.mongodb.dao.TriggerDao;
import com.netgrif.quartz.mongodb.util.QueryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerKey;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriggerStateManagerTest {

    @Mock
    private TriggerDao triggerDao;

    @Mock
    private JobDao jobDao;

    @Mock
    private PausedJobGroupsDao pausedJobGroupsDao;

    @Mock
    private PausedTriggerGroupsDao pausedTriggerGroupsDao;

    @Mock
    private QueryHelper queryHelper;

    @InjectMocks
    private TriggerStateManager stateManager;

    private static final TriggerKey triggerKey = new TriggerKey("t-name", "t-group");

    @BeforeEach
    void setUp() {
        stateManager = new TriggerStateManager(triggerDao, jobDao, pausedJobGroupsDao, pausedTriggerGroupsDao, queryHelper);
    }

    @Test
    void resetTriggerFromErrorStateShouldDoNothingWhenTriggerIsNotInErrorState() {
        when(triggerDao.getState(triggerKey)).thenReturn(Constants.STATE_WAITING);

        stateManager.resetTriggerFromErrorState(triggerKey);

        verifyNoInteractions(pausedTriggerGroupsDao);
        verifyNoMoreInteractions(triggerDao);
    }

    @Test
    void resetTriggerFromErrorStateShouldSetWaitingStateWhenTriggerIsInErrorState() {
        when(triggerDao.getState(triggerKey)).thenReturn(Constants.STATE_ERROR);
        HashSet<String> pausedGroups = new HashSet<>();
        pausedGroups.add("g1");
        doReturn(pausedGroups).when(pausedTriggerGroupsDao).getPausedGroups();

        stateManager.resetTriggerFromErrorState(triggerKey);

        verify(pausedTriggerGroupsDao).getPausedGroups();
        verify(triggerDao).transferState(triggerKey, Constants.STATE_ERROR, Constants.STATE_WAITING);
    }

    @Test
    void resetTriggerFromErrorStateShouldSetPausedStateWhenTriggerIsInPausedGroup() {
        when(triggerDao.getState(triggerKey)).thenReturn(Constants.STATE_ERROR);
        HashSet<String> pausedGroups = new HashSet<>();
        pausedGroups.add("g1");
        pausedGroups.add(triggerKey.getGroup());
        doReturn(pausedGroups).when(pausedTriggerGroupsDao).getPausedGroups();

        stateManager.resetTriggerFromErrorState(triggerKey);

        verify(pausedTriggerGroupsDao).getPausedGroups();
        verify(triggerDao).transferState(triggerKey, Constants.STATE_ERROR, Constants.STATE_PAUSED);
    }
}
