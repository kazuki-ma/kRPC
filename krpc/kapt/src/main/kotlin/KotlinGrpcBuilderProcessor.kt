import KotlinGrpcBuilderProcessor.Companion.KAPT_KOTLIN_CUSTOM_GENERATED_OPTION_NAME
import KotlinGrpcBuilderProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.example.GrpcServiceInterface
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement


/**
 * # Reference
 * * [square/kotlinpoet](https://github.com/square/kotlinpoet)
 */
@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_11) // to support Java 11
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME, KAPT_KOTLIN_CUSTOM_GENERATED_OPTION_NAME)
class KotlinGrpcBuilderProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_CUSTOM_GENERATED_OPTION_NAME = "kapt.kotlin.custom.generated"
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_CUSTOM_GENERATED_OPTION_NAME]
            ?: processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()

        check(generatedSourcesRoot.isNotEmpty())

        roundEnv.getElementsAnnotatedWith(GrpcServiceInterface::class.java).forEach { clz ->
            check(clz is TypeElement)

            generateBuilderClass(clz, generatedSourcesRoot)
        }

        return false
    }

    override fun getSupportedOptions(): Set<String> {
        return setOf("isolating")
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return mutableSetOf(GrpcServiceInterface::class.java.canonicalName)
    }

    private fun buildStubClass(
        clz: TypeElement,
        generatedSourcesRoot: String,
    ): TypeSpec.Builder {

        val stubBuilder =
            TypeSpec.classBuilder(ClassName(clz.asClassName().packageName, clz.asClassName().simpleName + "Stub"))
        val channelType = ClassName("io.grpc", "Channel")
        val callOptionsType = ClassName("io.grpc", "CallOptions")
        val coroutineStubType = ClassName("", "CoroutineStub")

        val builder = TypeSpec.classBuilder("CoroutineStub")
            .addSuperinterface(clz.asClassName())
            .superclass(
                ClassName(
                    "io.grpc.stub",
                    "AbstractBlockingStub"
                ).parameterizedBy(coroutineStubType)
            )
            .addSuperclassConstructorParameter("channel")
            .addSuperclassConstructorParameter("io.grpc.CallOptions.DEFAULT")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(ParameterSpec.builder("channel", channelType).build())
                    .build()
            )

        FunSpec.builder("build")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("channel", channelType)
            .addParameter("callOptions", callOptionsType)
            .returns(coroutineStubType)
            .addStatement("return CoroutineStub(channel)")
            .let { builder.addFunction(it.build()) }

        generatedSourcesRoot.length

        val methods =
            clz.enclosedElements.filter { it.kind == ElementKind.METHOD }.filterIsInstance<ExecutableElement>()

        methods.forEach { method ->
            FunSpec.builder(method.simpleName.toString())
                .addModifiers(KModifier.OVERRIDE) // , KModifier.SUSPEND)
                .addParameter("req", method.parameters[0].asKotlinTypeName())
                .returns(method.returnType.asTypeName())
                .addCode(
                    """
                    return io.grpc.stub.ClientCalls.blockingUnaryCall(
                        channel,
                        MethodDescriptor.newBuilder<%T, %T>()
                            .setType(MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName("${clz.asClassName().canonicalName}/${method.simpleName}")
                            .setRequestMarshaller(la.serendipity.krpc.JacksonMarshaler(%T::class.java))
                            .setResponseMarshaller(la.serendipity.krpc.JacksonMarshaler(%T::class.java))
                            .build(),
                        CallOptions.DEFAULT,
                        req,
                    )
                    """.trimIndent(),
                    method.parameters[0].asType().asTypeName(),
                    method.returnType.asTypeName(),
                    method.parameters[0].asType().asTypeName(),
                    method.returnType.asTypeName()
                )
                .let { builder.addFunction(it.build()) }
        }

        return stubBuilder
            .addType(builder.build())
    }

    private fun generateBuilderClass(
        clz: TypeElement,
        generatedSourcesRoot: String,
    ) {
        val packageName: String = clz.qualifiedName.removeSuffix("." + clz.simpleName).toString()
        val builderClassName: String = "${clz.simpleName}BindableService"

        val generatedBuilderType: TypeSpec.Builder = TypeSpec.classBuilder(builderClassName)
            .addSuperinterface(ClassName("io.grpc", "BindableService"))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(ParameterSpec.builder("delegated", clz.asKotlinTypeName()).build())
                    .build()
            )
            .addProperty(PropertySpec.builder("delegated", clz.asKotlinTypeName()).initializer("delegated").build())

        val methods =
            clz.enclosedElements.filter { it.kind == ElementKind.METHOD }.filterIsInstance<ExecutableElement>()

        val methodDescriptors = methods.map { method ->
            method to """
            addMethod(
                io.grpc.MethodDescriptor.newBuilder<%T, %T>()
                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("${clz.qualifiedName}/${method.simpleName}")
                    .setRequestMarshaller(la.serendipity.krpc.JacksonMarshaler(%T::class.java))
                    .setResponseMarshaller(la.serendipity.krpc.JacksonMarshaler(%T::class.java))
                    .build(),
                io.grpc.stub.ServerCalls.asyncUnaryCall(io.grpc.stub.ServerCalls.UnaryMethod<%T, %T> { request, responseObserver ->
                    runBlocking{ 
                        responseObserver.onNext(delegated.${method.simpleName}(request))
                        responseObserver.onCompleted()
                    }
                })
            ) 
            """.trimIndent()
        }

        FunSpec.builder("bindService")
            .addModifiers(KModifier.OVERRIDE)
            .returns(ClassName("io.grpc", "ServerServiceDefinition"))
            .addStatement(""" val builder = ServerServiceDefinition.builder("${clz.qualifiedName}")""")
            .apply {
                methodDescriptors.forEach { (method, body) ->
                    addStatement(
                        ".$body",
                        method.parameters[0].asType().asTypeName(),
                        method.returnType.asTypeName(),
                        method.parameters[0].asType().asTypeName(),
                        method.returnType.asTypeName(),
                        method.parameters[0].asType().asTypeName(),
                        method.returnType.asTypeName()
                    )
                }
            }
            .addStatement("return builder.build()")
            .apply {
                generatedBuilderType.addFunction(this.build())
            }

        val file = FileSpec.builder(packageName, builderClassName)
            .addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                    .addMember(
                        listOf(
                            "NOTHING_TO_INLINE",
                            "PLATFORM_CLASS_MAPPED_TO_KOTLIN",
                            "USELESS_CAST",
                            "RemoveRedundantQualifierName"
                        ).joinToString(prefix = "\"", separator = "\", \"", postfix = "\"")
                    )
                    .build()
            )
            .addImport("kotlin", "String")
            .addImport("io.grpc", "MethodDescriptor")
            .addImport("kotlinx.coroutines", "runBlocking")
            .addType(generatedBuilderType.build())
            .addType(buildStubClass(clz, generatedSourcesRoot).build())
            .build()

        val directory = File(generatedSourcesRoot).apply { exists() || mkdir() }
        file.writeTo(directory)
    }
}

private fun Element.asKotlinTypeName(): TypeName {
    return asType().asTypeName()
}
