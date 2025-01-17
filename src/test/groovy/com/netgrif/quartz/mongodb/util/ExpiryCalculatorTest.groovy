package com.netgrif.quartz.mongodb.util

import com.netgrif.quartz.mongodb.Clocks
import com.netgrif.quartz.mongodb.Constants
import com.netgrif.quartz.mongodb.cluster.Scheduler
import com.netgrif.quartz.mongodb.dao.SchedulerDao
import org.bson.Document
import spock.lang.Shared
import spock.lang.Specification

class ExpiryCalculatorTest extends Specification {

    @Shared def defaultInstanceId = "test instance"
    @Shared def jobTimeoutMillis = 100l
    @Shared def triggerTimeoutMillis = 10000l
    @Shared def isClustered = true

    def 'should tell if job lock has exired'() {
        given:
        def timeInPast = -10000L
        def clock = Clocks.constClock(101)
        def calc = createCalc(clock)
        def deadScheduler = createScheduler(timeInPast)
        def deadCalc = createCalc(clock, deadScheduler)

        expect: 'Expired lock: 101 - 0 > 100 (timeout)'
        calc.isJobLockExpired(createDoc(0))
        deadCalc.isJobLockExpired(createDoc(timeInPast))

        and: 'Not expired: 101 - 1/101 <= 100'
        !calc.isJobLockExpired(createDoc(1))
        !calc.isJobLockExpired(createDoc(101))

        and: 'CheckIn expired'
        deadCalc.isJobLockExpired(createDoc(10))
    }

    def 'should tell if trigger lock has expired'() {
        given:
        def clock = Clocks.constClock(10001l)

        when: 'Tests for alive scheduler'
        def aliveScheduler = createScheduler(5000) // lastCheckinTime = 5000
        def calc = createCalc(clock, aliveScheduler)

        then: 'Expired lock: 10001 - 0 > 10000 (timeout)'
        calc.isTriggerLockExpired(createDoc(0))

        and: 'Not expired: 101 - 1/10001 <= 10000'
        !calc.isTriggerLockExpired(createDoc(1))
        !calc.isTriggerLockExpired(createDoc(10001))

        when: 'Tests for dead scheduler'
        def deadScheduler = createScheduler(0) // lastCheckinTime = 0
        def deadCalc = createCalc(clock, deadScheduler)

        then: 'Expired lock: 10001 - 0 > 10000 (timeout)'
        deadCalc.isTriggerLockExpired(createDoc(0))

        and: 'Not expired: 10001 - 1/10001 <= 10000'
        deadCalc.isTriggerLockExpired(createDoc(1))
        deadCalc.isTriggerLockExpired(createDoc(10001))
    }

    def Scheduler createScheduler() {
        createScheduler(100l)
    }

    def createScheduler(long lastCheckinTime) {
        new Scheduler("sname", defaultInstanceId, lastCheckinTime, 100l)
    }

    def SchedulerDao createSchedulerDao(Scheduler scheduler) {
          Mock(SchedulerDao) {
            findInstance(_ as String) >> scheduler
            isNotSelf(scheduler) >> true
        }
    }

    def createCalc(Clock clock) {
        createCalc(clock, createScheduler())
    }

    def createCalc(Clock clock, Scheduler scheduler) {
        new ExpiryCalculator(createSchedulerDao(scheduler), clock,
                jobTimeoutMillis, triggerTimeoutMillis,isClustered)
    }

    def createDoc(long lockTime) {
        new Document([(Constants.LOCK_TIME)       : new Date(lockTime),
                      (Constants.LOCK_INSTANCE_ID): defaultInstanceId])
    }
}
