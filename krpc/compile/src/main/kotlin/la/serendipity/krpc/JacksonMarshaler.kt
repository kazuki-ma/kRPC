package la.serendipity.krpc

import java.io.InputStream

class JacksonMarshaler<T>(val clazz: Class<T>) : io.grpc.MethodDescriptor.Marshaller<T> {
    override fun stream(value: T): InputStream {
        TODO("Compile library")
    }

    override fun parse(stream: InputStream): T {
        TODO("Compile library")
    }
}