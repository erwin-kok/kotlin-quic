package org.erwinkok.quic.common.quiche

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

object NativeHelper {
    enum class PlatformType {
        LINUX,
        MAC,
        WINDOWS,
    }

    val C_BYTE: ValueLayout.OfByte = ValueLayout.JAVA_BYTE
    val C_BOOL: ValueLayout.OfByte = ValueLayout.JAVA_BYTE
    val C_SHORT: ValueLayout.OfShort = ValueLayout.JAVA_SHORT
    val C_INT: ValueLayout.OfInt = ValueLayout.JAVA_INT
    val C_LONG: ValueLayout.OfLong = ValueLayout.JAVA_LONG
    val C_POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.Companion.MAX_VALUE, ValueLayout.JAVA_BYTE))
    val Platform: PlatformType

    private val SYMBOL_LOOKUP = SymbolLookup.loaderLookup().or(Linker.nativeLinker().defaultLookup())

    init {
        var arch = System.getProperty("os.arch")
        if ("x86_64" == arch || "amd64" == arch) {
            arch = "x86-64"
        }
        val osName = System.getProperty("os.name")
        val prefix = if (osName.startsWith("Linux")) {
            Platform = PlatformType.LINUX
            "linux-$arch"
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            Platform = PlatformType.MAC
            "darwin-$arch"
        } else if (osName.startsWith("Windows")) {
            Platform = PlatformType.WINDOWS
            "win32-$arch"
        } else {
            throw UnsatisfiedLinkError("Unsupported OS: $osName")
        }
        loadNativeLibrary(prefix)
    }

    val isLinux = Platform == PlatformType.LINUX
    val isMac = Platform == PlatformType.MAC
    val isWindows = Platform == PlatformType.WINDOWS

    fun downcallHandle(symbol: String, functionDescriptor: FunctionDescriptor): MethodHandle {
        val address = SYMBOL_LOOKUP.find(symbol)
            .orElseThrow { UnsatisfiedLinkError("unresolved symbol: $symbol") }
        return Linker.nativeLinker().downcallHandle(address, functionDescriptor)
    }

    inline fun <reified T> upcallMemorySegment(methodName: String, instance: T, functionDescriptor: FunctionDescriptor, scope: Arena): MemorySegment {
        return upcallMemorySegment(T::class.java, methodName, instance, functionDescriptor, scope)
    }

    fun <T> upcallMemorySegment(clazz: Class<T>, methodName: String, instance: T, functionDescriptor: FunctionDescriptor, scope: Arena): MemorySegment {
        try {
            var handle = MethodHandles.lookup().findVirtual(clazz, methodName, functionDescriptor.toMethodType())
            handle = handle.bindTo(instance)
            return Linker.nativeLinker().upcallStub(handle, functionDescriptor, scope)
        } catch (e: ReflectiveOperationException) {
            throw AssertionError(e)
        }
    }

    private fun loadNativeLibrary(prefix: String) {
        try {
            val libName = prefix + "/" + System.mapLibraryName("quiche")
            val target = createTempDirectory().resolve(libName)
            Files.createDirectories(target.parent)

            val classLoader = NativeHelper::class.java.classLoader
            val resourceAsStream = classLoader.getResourceAsStream(libName)
            if (resourceAsStream == null) {
                throw UnsatisfiedLinkError("Cannot find quiche native library for architecture $prefix from resources")
            }

            resourceAsStream.use { input ->
                Files.newOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }

            System.load(target.toAbsolutePath().toString())
            target.toFile().deleteOnExit()
        } catch (e: Throwable) {
            throw UnsatisfiedLinkError("Cannot find load native library for architecture $prefix").initCause(e)
        }
    }
}
