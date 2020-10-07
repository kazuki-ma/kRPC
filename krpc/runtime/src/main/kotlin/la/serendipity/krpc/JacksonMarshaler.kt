package la.serendipity.krpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.ByteArrayInputStream
import java.io.InputStream

class JacksonMarshaler<T>(val clazz: Class<T>) : io.grpc.MethodDescriptor.Marshaller<T> {
    companion object {
        private val objectMapper = ObjectMapper()
            .registerModule(KotlinModule())
    }

    override fun stream(value: T): InputStream {
        return ByteArrayInputStream(objectMapper.writeValueAsBytes(value))
    }

    override fun parse(stream: InputStream): T {
        return objectMapper.readValue(stream, clazz)
    }
}