package xyz.malkki.gtfsapi.utils

import reactor.core.publisher.Flux
import xyz.malkki.gtfsapi.extensions.debounce
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.io.path.inputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private fun Path.pollModifications(pollInterval: Duration): Flux<Unit> = Flux.create { emitter ->
    var previousHash = getContentHash()

    val thread = thread {
        while (!Thread.currentThread().isInterrupted) {
            val newHash = getContentHash()
            if (previousHash != newHash) {
                emitter.next(Unit)

                previousHash = newHash
            }

            Thread.sleep(pollInterval.inWholeMilliseconds)
        }
    }

    emitter.onDispose { thread.interrupt() }
}

private fun Path.getContentHash(): BigInteger {
    DigestInputStream(inputStream().buffered(), MessageDigest.getInstance("MD5")).use {
        val buf = ByteArray(1024)

        //This while block is needed to fully read the file in small segments
        while (it.read(buf) != -1) {

        }

        return BigInteger(1, it.messageDigest.digest())
    }
}

fun Path.watchModifications(pollInterval: Duration): Flux<Unit> = Flux.merge(
    pollModifications(pollInterval),
    watchFile(StandardWatchEventKinds.ENTRY_MODIFY).debounce(500.milliseconds).map {}
)