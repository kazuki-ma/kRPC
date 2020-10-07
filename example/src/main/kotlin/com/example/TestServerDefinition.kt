package com.example

import com.linecorp.armeria.client.Clients
import com.linecorp.armeria.common.logging.LogLevel
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.LoggingService
import org.slf4j.LoggerFactory

// Define our services
@GrpcServiceInterface
interface MyTestService {
    fun welcomeMessageA(req: WelcomeMessageRequest): WelcomeMessageResponse

    data class WelcomeMessageRequest(
        val myName: String,
    )

    data class WelcomeMessageResponse(
        val message: String,
    )
}

// Server implementation
object Impl : MyTestService {
    override fun welcomeMessageA(req: MyTestService.WelcomeMessageRequest): MyTestService.WelcomeMessageResponse {
        return MyTestService.WelcomeMessageResponse("Hello, ${req.myName}.")
    }
}

var log = LoggerFactory.getLogger("root")

fun main() {
    // Start server.
    val server = com.linecorp.armeria.server.Server.builder()
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

    server.start().get()


    // Define client.
    val newClient: MyTestService = Clients.newClient(
        "gproto+http://127.0.0.1:${server.activeLocalPort()}/",
        MyTestServiceStub.CoroutineStub::class.java
    )


    // Call server via client.
    val response: MyTestService.WelcomeMessageResponse =
        newClient.welcomeMessageA(MyTestService.WelcomeMessageRequest(myName = "Server"))
    log.info("{}", response)

    server.stop()
}

