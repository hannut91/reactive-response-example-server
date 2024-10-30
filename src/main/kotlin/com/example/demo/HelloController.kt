package com.example.demo

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RestController
class HelloController {
    var sink: FluxSink<DataBuffer>? = null
    var sink2: FluxSink<Publisher<out DataBuffer>>? = null

    var sink3: FluxSink<DataBuffer>? = null

    // 응답이 Mono<Void>인 경우
    @PostMapping("/response/mono/void")
    suspend fun responseMonoVoid(
        httpReq: ServerHttpRequest,
        httpRes: ServerHttpResponse
    ): Mono<Void> {
        var sink: FluxSink<DataBuffer>? = null
        val flux = Flux.create {
            sink = it
        }

        httpReq.body.publishOn(Schedulers.parallel())
            .subscribe({dataBuffer ->
                val byteArray = ByteArray(dataBuffer.readableByteCount())
                    .also { dataBuffer.read(it) }
                DataBufferUtils.release(dataBuffer)

                val text = String(byteArray, StandardCharsets.UTF_8)
                println("received : $text")

                sink?.next(DefaultDataBufferFactory().wrap("{\"received\": \"${text}\"}\n".toByteArray()))
            }, {

            }, {
                sink?.complete()
            })

        return httpRes.writeWith(flux)
    }

    @GetMapping("/hello", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun hello(
        httpRes: ServerHttpResponse
    ): Mono<Void> {
        val f: Flux<DataBuffer> = Flux.create {
            println("33")
            sink = it
        }
        println("11")
        httpRes.writeAndFlushWith(Flux.just(f)).subscribe {
            println("subscribe is called")
        }
        println("22")
        sink!!.next(
            DefaultDataBufferFactory().wrap(
                "111\n"
                    .toByteArray()
            )
        )
        delay(1000)
        sink!!.next(
            DefaultDataBufferFactory().wrap(
                "222\n"
                    .toByteArray()
            )
        )
        return Mono.empty()
    }

    @GetMapping("/hello5", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun hello5(
        httpRes: ServerHttpResponse
    ): Mono<Void> {
        return httpRes.writeAndFlushWith(Flux.create {
            println("11")
            sink2 = it
        })
        println("22")
        sink2!!.next(
            Flux.just(
                DefaultDataBufferFactory().wrap(
                    "111\n"
                        .toByteArray()
                )
            )
        )
        runBlocking {
            delay(1000)
        }
        sink2!!.next(
            Flux.just(
                DefaultDataBufferFactory().wrap(
                    "222\n"
                        .toByteArray()
                )
            )
        )
//        it.complete()
        return Mono.empty()
    }

    @GetMapping("/hello2", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun hello(): Flux<String> {
        return Flux.interval(Duration.ofSeconds(1))
            .map { "Hello from duplex communication! $it" }
    }

    @GetMapping("/hello3")
    fun hello3(): Flow<String> {
        return flow {
            for (i in 1..10) {
                delay(1000)
                emit("Hello from duplex communication! $i")
            }
        }
    }

    @GetMapping("/hello4")
    fun hello4() = Flux.create {
        it.next("Hello")
        runBlocking {
            delay(1000)
        }
        it.next("World")
        runBlocking {
            delay(1000)
        }
        it.next("!!!")
        it.complete()
    }

    @GetMapping("/hello6")
    fun hello6(httpRes: ServerHttpResponse): Mono<Void> {
        mono {
            null
        }.subscribe({
            println("subscribe is called")
        }, {

        }, {
            println("complete")
        })
        return Mono.empty()
    }

    @PostMapping("/demo")
    fun demo(
        httpReq: ServerHttpRequest,
        httpRes: ServerHttpResponse
    ): Mono<Void> {
        httpRes.writeAndFlushWith(Flux.just(Flux.create {
            println("create-1")
            sink3 = it
            it.next(DefaultDataBufferFactory().wrap("111\n".toByteArray()))
            println("create-2")
        }))
        return Mono.create { sink ->
            httpReq.body.publishOn(Schedulers.parallel())
                .flatMap { dataBuffer ->
                    mono {
                        val b = ByteArray(dataBuffer.readableByteCount())
                            .also { dataBuffer.read(it) }
                        val text = String(b, StandardCharsets.UTF_8)
                        sink3?.next(DefaultDataBufferFactory().wrap("resulttt"
                            .toByteArray()))
                    }
                }.then(mono {
                    sink3?.complete()
                })
                .subscribe {
                    sink.success()
                    println("complete-3")
                }
        }

    }

    @GetMapping("/void")
    fun void(
        httpReq: ServerHttpRequest,
        httpRes: ServerHttpResponse
    ): Mono<Void> {
        return Mono.empty()
    }

    @GetMapping("/simple")
    fun simple(
        httpReq: ServerHttpRequest,
        httpRes: ServerHttpResponse
    ): String {
        return "리턴 데이터"
    }

    @GetMapping("/wrong")
    suspend fun wrong(
        httpReq: ServerHttpRequest,
        httpRes: ServerHttpResponse
    ): Flow<String> {
        GlobalScope.launch {
            delay(1000)
            println("writeWith is called")
            println(httpRes)
            httpRes.writeWith(
                Mono.just(
                    DefaultDataBufferFactory().wrap("Hello world".toByteArray())
                )
            ).subscribe()
        }

        return flow {
//            delay(2000)
            emit("Hello")
        }
    }

    @PostMapping("/full")
    suspend fun full(
        httpReq: ServerHttpRequest,
        @RequestBody
        incomingData: Flux<String>
    ): Flow<String> {
        println("is called")
//        return incomingData.publishOn(Schedulers.parallel())
//            .asFlow()
//            .flatMapMerge {
//                println("data : $it")
//                flow {
//                    emit("Hello")
//                }
//            }

            return httpReq.body.publishOn(
                Schedulers.parallel()
            ).asFlow()
                .flatMapConcat {dataBuffer ->
                    val bytes = ByteArray(dataBuffer.readableByteCount())
                    dataBuffer.read(bytes)
                    DataBufferUtils.release(dataBuffer)
                    println("data : ${String(bytes, StandardCharsets.UTF_8)}")
                    flow {
                        delay(500)
                        emit("received ${String(bytes, StandardCharsets.UTF_8)}")
                    }
                }
    }
}
