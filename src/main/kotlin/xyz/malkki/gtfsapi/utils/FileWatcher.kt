package xyz.malkki.gtfsapi.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Flux
import java.nio.file.ClosedWatchServiceException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.WatchEvent
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

fun Path.watchFile(vararg events: WatchEvent.Kind<*>): Flux<WatchEvent<*>> = Flux.create { emitter ->
    val watchFile = if (Files.isSymbolicLink(this)) {
        Files.readSymbolicLink(this)
    } else {
        this
    }
    val watchDir = watchFile.parent

    val watchThread = thread {
        val watchService = fileSystem.newWatchService()!!
        val watchKey = watchDir.register(watchService, events)

        while (true) {
            try {
                val watchKey = watchService.take()
                watchKey.pollEvents().forEach {
                    if (it.context() == watchFile) {
                        emitter.next(it)
                    }
                }
                if (!watchKey.reset()) {
                    log.warn { "Could not reset file watcher on path ${watchDir.toAbsolutePath()}. Maybe the path is not accessible anymore?" }
                }
            } catch (cwse: ClosedWatchServiceException) {
                //Ignored
            } catch (ie: InterruptedException) {
                break
            }
        }

        watchKey.cancel()
        watchService.close()
    }

    emitter.onDispose { watchThread.interrupt() }
}