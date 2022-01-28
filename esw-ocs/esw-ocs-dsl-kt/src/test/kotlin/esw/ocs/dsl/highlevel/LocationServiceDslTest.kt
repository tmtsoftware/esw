package esw.ocs.dsl.highlevel

import akka.Done
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.IRegistrationResult
import csw.location.api.models.*
import csw.location.api.models.Connection.HttpConnection
import esw.ocs.dsl.highlevel.models.*
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import msocket.api.Subscription
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LocationServiceDslTest : LocationServiceDsl {
    override val locationService: ILocationService = mockk()
    override val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val prefix = Prefix("IRIS.motor")
    private val componentId = ComponentId(prefix, Service)
    private val httpConnection = HttpConnection(componentId)
    private val httpRegistration = HttpRegistration(httpConnection, 8080, "/")
    private val registrationResult: IRegistrationResult = mockk()
    private val httpLocation: HttpLocation = mockk()

    @AfterEach
    fun clearMocks() = clearAllMocks()

    @Test
    fun `register should call underlying register method from LocationService | ESW-277`() = runBlocking {
        every { locationService.register(httpRegistration) } answers { completedFuture(registrationResult) }
        every { registrationResult.location() } answers { httpLocation }
        every { registrationResult.unregister() } answers { completedFuture(Done.done()) }

        val result: RegistrationResult = register(httpRegistration)
        result.location shouldBe registrationResult.location()
        verify { locationService.register(httpRegistration) }

        result.unregister()
        verify { registrationResult.unregister() }
    }

    @Test
    fun `unregister should call underlying unregister method from LocationService | ESW-277`() = runBlocking {
        every { locationService.unregister(httpConnection) } answers { completedFuture(Done.done()) }

        unregister(httpConnection)
        verify { locationService.unregister(httpConnection) }
    }

    @Test
    fun `findLocation should call underlying find method from LocationService | ESW-277`() = runBlocking {
        every { locationService.find(httpConnection) } answers { completedFuture(Optional.of(httpLocation)) }

        findLocation(httpConnection) shouldBe httpLocation
        verify { locationService.find(httpConnection) }
    }

    @Test
    fun `resolveLocation should call underlying resolve method from LocationService | ESW-277`() = runBlocking {
        val timeoutK = 1.seconds
        val timeoutJ = timeoutK.toJavaDuration()
        every { locationService.resolve(httpConnection, timeoutJ) } answers { completedFuture(Optional.of(httpLocation)) }

        resolveLocation(httpConnection, timeoutK) shouldBe httpLocation
        verify { locationService.resolve(httpConnection, timeoutJ) }
    }


    @Test
    fun `listLocations should call underlying list method from LocationService | ESW-277`() = runBlocking {
        val mockedLocations: List<Location> = List(10) { httpLocation }
        every { locationService.list() } answers { completedFuture(mockedLocations) }

        listLocations() shouldBe mockedLocations
        verify { locationService.list() }
    }

    @Test
    fun `listLocationsBy component type should call underlying list(componentType) method from LocationService | ESW-277`() = runBlocking {
        val componentType = HCD
        val mockedLocations: List<Location> = List(10) { httpLocation }
        every { locationService.list(componentType) } answers { completedFuture(mockedLocations) }

        listLocationsBy(componentType) shouldBe mockedLocations
        verify { locationService.list(componentType) }
    }

    @Test
    fun `listLocationsBy connection type should call underlying list(connectionType) method from LocationService | ESW-277`() = runBlocking {
        val connectionType = HttpType
        val mockedLocations: List<Location> = List(10) { httpLocation }
        every { locationService.list(connectionType) } answers { completedFuture(mockedLocations) }

        listLocationsBy(connectionType) shouldBe mockedLocations
        verify { locationService.list(connectionType) }
    }

    @Test
    fun `listLocationsBy hostname should call underlying list(hostname) method from LocationService | ESW-277`() = runBlocking {
        val hostname = "10.1.1.1"
        val mockedLocations: List<Location> = List(10) { httpLocation }
        every { locationService.list(hostname) } answers { completedFuture(mockedLocations) }

        listLocationsByHostname(hostname) shouldBe mockedLocations
        verify { locationService.list(hostname) }
    }

    @Test
    fun `listLocationsBy prefix should call underlying listByPrefix method from LocationService | ESW-277`() = runBlocking {
        val mockedLocations: List<Location> = List(10) { httpLocation }
        every { locationService.listByPrefix(prefix.toString()) } answers { completedFuture(mockedLocations) }

        listLocationsBy(prefix.toString()) shouldBe mockedLocations
        verify { locationService.listByPrefix(prefix.toString()) }
    }

    @Test
    fun `onLocationTrackingEvent should call underlying subscribe method from LocationService | ESW-277`() = runBlocking {
        val mockedSubscription: Subscription = mockk()
        every { locationService.subscribe(httpConnection, any()) } answers { mockedSubscription }

        val receivedEvents = mutableListOf<TrackingEvent>()
        onLocationTrackingEvent(httpConnection) { receivedEvents += it }
        verify { locationService.subscribe(httpConnection, any()) }
    }
}
