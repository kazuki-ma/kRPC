package com.example

import com.example.MyTestService.WelcomeMessageRequest
import com.example.MyTestService.WelcomeMessageResponse
import com.linecorp.armeria.client.Clients
import com.linecorp.armeria.common.logging.LogLevel
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.LoggingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

// Define our services
@GrpcServiceInterface
interface MyTestService {
    fun welcomeMessage(req: WelcomeMessageRequest): WelcomeMessageResponse

    data class WelcomeMessageRequest(
        val myName: String,
    )

    data class WelcomeMessageResponse(
        val message: String,
    )
}

// Server implementation
object Impl : MyTestService {
    override fun welcomeMessage(req: WelcomeMessageRequest): WelcomeMessageResponse {
        return WelcomeMessageResponse("Hello, ${req.myName}.")
    }
}


class TestMain {
    private var log = LoggerFactory.getLogger(javaClass)

    private val server = com.linecorp.armeria.server.Server.builder()
        .service(
            GrpcService.builder()
                .addService(MyTestServiceBindableService(Impl))
                .build()
        )
        .decorator(
            LoggingService.builder()
                .samplingRate(1.0f)
                .requestLogLevel(LogLevel.INFO)
                .successfulResponseLogLevel(LogLevel.INFO)
                .failureResponseLogLevel(LogLevel.INFO)
                .newDecorator()
        )
        .build()

    @BeforeEach
    fun setUp() {
        // Start server.
        server.start().get()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }


    @Test
    fun test() {
        // Define client.
        val newClient: MyTestService = Clients.newClient(
            "gproto+http://127.0.0.1:${server.activeLocalPort()}/",
            MyTestServiceStub.CoroutineStub::class.java
        )

        // Do
        val request = WelcomeMessageRequest(myName = "Server")
        val response = newClient.welcomeMessage(request)

        // Verify
        log.info("{}", response)
    }
}
