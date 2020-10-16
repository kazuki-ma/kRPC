Example
=====


Define Interface
----
```kotlin
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
```

Implement Server
----
```kotlin
// Server implementation
object Impl : MyTestService {
    override fun welcomeMessage(req: WelcomeMessageRequest): WelcomeMessageResponse {
        return WelcomeMessageResponse("Hello, ${req.myName}.")
    }
}
```

Just call
----
```kotlin
val request = WelcomeMessageRequest(myName = "Server")
val response = newClient.welcomeMessage(request)
```

Results
----
```
21:02:28.749 [er-nio-2-2] INFO  c.l.a.s.logging.LoggingService - [sreqId=f3f642bb, chanId=ff55c55b, raddr=127.0.0.1:62943, laddr=127.0.0.1:62942][h2c://w010d72001311m.local/com.example.MyTestService/welcomeMessage#POST] Request: {startTime=2020-10-16T12:02:28.660Z(1602849748660027), length=24B, duration=87917µs(87917647ns), scheme=gproto+h2c, name=welcomeMessage, headers=[:method=POST, :path=/com.example.MyTestService/welcomeMessage, :scheme=http, :authority=127.0.0.1:62942, content-type=application/grpc+proto, te=trailers, grpc-accept-encoding=gzip, grpc-timeout=15000000u, user-agent=armeria/1.0.0], content=DefaultRpcRequest{serviceType=GrpcLogUtil, method=com.example.MyTestService/welcomeMessage, params=[WelcomeMessageRequest(myName=Server)]}}  at com.linecorp.armeria.common.logging.LogLevel.log(LogLevel.java:152)
21:02:28.757 [er-nio-2-2] INFO  c.l.a.s.logging.LoggingService - [sreqId=f3f642bb, chanId=ff55c55b, raddr=127.0.0.1:62943, laddr=127.0.0.1:62942][h2c://w010d72001311m.local/com.example.MyTestService/welcomeMessage#POST] Response: {startTime=2020-10-16T12:02:28.728Z(1602849748728851), length=33B, duration=28324µs(28324119ns), totalDuration=97147µs(97147685ns), headers=[:status=200, content-type=application/grpc+proto, grpc-encoding=identity, grpc-accept-encoding=gzip], content=CompletableRpcResponse{WelcomeMessageResponse(message=Hello, Server.)}, trailers=[EOS, grpc-status=0]}  at com.linecorp.armeria.common.logging.LogLevel.log(LogLevel.java:152)
```

You can see complete example on [example/src/test/kotlin/com/example/TestServerDefinition.kt](example/src/test/kotlin/com/example/TestServerDefinition.kt)