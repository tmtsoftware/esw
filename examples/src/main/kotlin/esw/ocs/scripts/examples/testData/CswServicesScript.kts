package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script

script { cswServices ->
    // this is just to verify that csw services are available in script
    cswServices.logger
    cswServices.alarmService
    cswServices.eventService
    cswServices.eventPublisher
    cswServices.eventSubscriber
    cswServices.locationService
    cswServices.configService
    cswServices.timeService
    cswServices.databaseServiceFactory
}