@file:OptIn(ExperimentalCoroutinesApi::class)
@file:Suppress("DuplicatedCode", "SpellCheckingInspection")

package com.lagostout.flow

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.cancellation.CancellationException

suspend fun <A, B> Flow<A>.takeUntilUsingChannelFlowAndTerminatingOnOtherFlowFirstEmissionOrCompletion(other: Flow<B>): Flow<A> =
    channelFlow {
        val job = launch {
            collect {
                send(it)
            }
        }
        other.firstOrNull()
        job.cancel()
    }

suspend fun <A, B> Flow<A>.takeUntilUsingFlatMapLatest(other: Flow<B>): Flow<A> =
    flow {
        emit(this@takeUntilUsingFlatMapLatest)
        other.firstOrNull()
        emit(emptyFlow())
    }.flatMapLatest { it }

suspend fun <A> Flow<A>.dropAll(): Flow<A> =
    flow {
        collect { }
    }

suspend fun <A> Flow<A>.emitOnCompletionJust(a: A): Flow<A> =
    dropAll().onCompletion { emit(a) }

// region amb

fun <A> Flow<A>.amb(other: Flow<A>): Flow<A> {
    return channelFlow {
        lateinit var j2: Job
        val j1 = launch {
            other.collect {
                j2.cancel()
                send(it)
            }
        }
        j2 = launch {
            this@amb.collect {
                j1.cancel()
                send(it)
            }
        }
    }
}

fun <A> Flow<A>.ambUsingVarargAndChannelFlow(vararg others: Flow<A>): Flow<A> {
    return channelFlow {
        lateinit var jobs: List<Job>
        jobs = (listOf(this@ambUsingVarargAndChannelFlow) + others).mapIndexed { index1, flow ->
            launch(start = CoroutineStart.LAZY) {
                flow.collectIndexed { index, a ->
                    if (index == 0) {
                        jobs.filterIndexed { index2, _ -> index1 != index2 }
                            .forEach { it.cancel() }
                    }
                    send(a)
                }
            }
        }
        jobs.forEach {
            it.start()
        }
    }
}

// NEXT - Test!!

fun <A> Flow<A>.ambUsingVararg(vararg others: Flow<A>): Flow<A> {
    return flow {
        coroutineScope {
            lateinit var jobs: List<Job>
            jobs = (listOf(this@ambUsingVararg) + others).mapIndexed { index1, flow ->
                launch(start = CoroutineStart.LAZY) {
                    flow.collectIndexed { index, a ->
                        if (index == 0) {
                            jobs.filterIndexed { index2, _ -> index1 != index2 }
                                .forEach { it.cancel() }
                        }
                        emit(a)
                    }
                }
            }
            jobs.forEach {
                it.start()
            }
        }
    }
}

fun <A> Flow<A>.ambUsingChannelsAndIgnoringEmptyFlows(vararg others: Flow<A>): Flow<A> {
    return channelFlow {
        val flows = listOf(this@ambUsingChannelsAndIgnoringEmptyFlows) + others
        val channels: List<ReceiveChannel<Pair<A, Int>>> =
            flows.mapIndexed { index, flow ->
                produce {
                    flow.onCompletion {
                        println("completed $flow $index")
                    }.collect {
                        println("sending $it")
                        send(Pair(it, index))
                    }
                }
            }
        val ch = Channel<Pair<A, Int>>()
        channels.forEach { channel ->
            launch {
                try {
                    val ai = channel.receive()
                    ch.send(ai)
                } catch (e: ClosedReceiveChannelException) {
                    throw CancellationException()
                }
            }
        }
        val (a, index) = ch.receive()
        channels.forEachIndexed { i, channel ->
            if (i != index) {
                println("cancelling channel $i")
                channel.cancel()
            }
        }
        send(a)
        for ((aa, _) in channels[index]) {
            send(aa)
        }
    }
}

fun <A> Flow<A>.ambUsingChannelsAndEmptyFlowCanWin(vararg others: Flow<A>): Flow<A> {
    return channelFlow {
        val flows = listOf(this@ambUsingChannelsAndEmptyFlowCanWin) + others
        val channels: List<ReceiveChannel<Pair<A, Int>>> =
            flows.mapIndexed { index, flow ->
                produce {
                    flow.onCompletion {
                        println("completed $flow $index")
                    }.collect {
                        println("sending $it")
                        send(Pair(it, index))
                    }
                }
            }
        val ch = Channel<Pair<A, Int>>()
        channels.forEach { channel ->
            launch {
                try {
                    val ai = channel.receive()
                    ch.send(ai)
                } catch (e: ClosedReceiveChannelException) {
                    this@channelFlow.close()
                }
            }
        }
        val (a, index) = ch.receive()
        channels.forEachIndexed { i, channel ->
            if (i != index) {
                println("cancelling channel $i")
                channel.cancel()
            }
        }
        send(a)
        for ((aa, _) in channels[index]) {
            send(aa)
        }
    }
}

// To use with a flow of nullable type, map the flow to options.
// This is necessary because of the ambiguity introduced by using
// ChannelResult to read channels.  The value in ChannelResult could
// be null if there's been an exception, or if the channel's values
// are actually nullable - we can't tell the difference.  So we restrict
// A to be non-nullable. Hence, A & Any.
fun <A> Flow<A>.ambUsingSelect(vararg others: Flow<A & Any>): Flow<A> =
    flow {
        coroutineScope {
            val flows = listOf(this@ambUsingSelect) + others
            val channels: List<ReceiveChannel<A>> =
                flows.map { flow ->
                    produce {
                        flow.onCompletion {
                            println("completed $flow ${flows.indexOf(flow)} with exception $it")
                        }.collect {
                            send(it)
                        }
                    }
                }
            val first: Pair<A?, ReceiveChannel<A>> = select {
                for (channel in channels) {
                    channel.onReceiveCatching {
                        Pair(it.getOrNull(), channel)
                    }
                }
            }
            val (a, winner) = first
            channels.filterNot { it == winner }
                .forEach {
                    it.cancel()
                }
            a!!.let {
                emit(a)
                for (aa in winner) {
                    emit(aa)
                }
            }
        }
    }

fun <A> Flow<A>.ambUsingSelectAndDroppingFailingFlowsFromRace(vararg others: Flow<A>): Flow<A> =
    flow {
        supervisorScope {
            val flows = listOf(this@ambUsingSelectAndDroppingFailingFlowsFromRace) + others
            var channels: List<ReceiveChannel<A>> =
                flows.map { flow ->
                    produce {
                        flow.onCompletion {
                            println("completed $flow ${flows.indexOf(flow)} with exception $it")
                        }.collect {
                            send(it)
                        }
                    }
                }
            var  first: Pair<ChannelResult<A>, ReceiveChannel<A>>
            do {
                first = select {
                    for (channel in channels) {
                        channel.onReceiveCatching {
                            Pair(it, channel)
                        }
                    }
                }
                if (first.first.exceptionOrNull() != null)
                    channels = channels - first.second
                else break
            } while (channels.isNotEmpty())
            val (channelResult, winningChannel) = first
            channels.filterNot { it == winningChannel }
                .forEach {
                    it.cancel()
                }
            if (!channelResult.isClosed) {
                channelResult.let {
                    // Should never actually throw, because winning channel is not closed,
                    // which it would be if channel was empty or had thrown an exception.
                    emit(channelResult.getOrThrow())
                    for (aa in winningChannel) {
                        emit(aa)
                    }
                }
            }
        }
    }

fun <A> Flow<A>.ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures(vararg others: Flow<A>): Flow<Either<Throwable, A>> =
    flow {
        supervisorScope {
            val flows = listOf(this@ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures) + others
            var channels: List<ReceiveChannel<A>> =
                flows.map { flow ->
                    produce {
                        flow.onCompletion {
                            println("completed $flow ${flows.indexOf(flow)} with exception $it")
                        }.collect {
                            send(it)
                        }
                    }
                }
            var first: Pair<ChannelResult<A>, ReceiveChannel<A>>
            val failures = mutableListOf<Either.Left<Throwable>>()
            do {
                first = select {
                    println("select")
                    for (channel in channels) {
                        channel.onReceiveCatching {
                            Pair(it, channel)
                        }
                    }
                }
                val e = first.first.exceptionOrNull()
                if (e != null) {
                    channels = channels - first.second
                    failures.add(Either.Left(e))
                }
                else break
            } while (channels.isNotEmpty())
            val (channelResult, winningChannel) = first
            channels.filterNot { it == winningChannel }
                .forEach {
                    it.cancel()
                }
            channelResult.let {
                // Should never actually throw, because winning channel is not closed,
                // which it would be if channel was empty or had thrown an exception.
                emitAll(failures.asFlow())
                if (!channelResult.isClosed) {
                    emit(channelResult.getOrThrow().right())
                    for (aa in winningChannel) {
                        emit(aa.right())
                    }
                }
            }
        }
    }

fun <A> Flow<A>.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(vararg others: Flow<A>): Flow<Either<Throwable, A>> =
    flow {
        supervisorScope {
            val flows = listOf(this@ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures) + others
            var channels: List<ReceiveChannel<A>> =
                flows.map { flow ->
                    produce {
                        flow.onCompletion {
                            println("completed $flow ${flows.indexOf(flow)} with exception $it")
                        }.collect {
                            send(it)
                        }
                    }
                }
            var first: Pair<ChannelResult<A>, ReceiveChannel<A>>
            val failures = mutableListOf<Either.Left<Throwable>>()
            do {
                first = select {
                    for (channel in channels) {
                        channel.onReceiveCatching {
                            println()
                            Pair(it, channel)
                        }
                    }
                }
                val (channelResult, channel) = first
                val e = channelResult.exceptionOrNull()
                if (e != null || channelResult.isClosed) {
                    channels = channels - channel
                    if (e != null) failures.add(Either.Left(e))
                } else {
                    break
                }
            } while (channels.isNotEmpty())
            val (channelResult, channel) = first
            channels.filterNot { it == channel }
                .forEach {
                    it.cancel()
                }
            channelResult.let {
                // Should never actually throw, because winning channel is not closed,
                // which it would be if channel was empty or had thrown an exception.
                emitAll(failures.asFlow())
                if (!channelResult.isClosed) {
                    emit(channelResult.getOrThrow().right())
                    for (aa in channel) {
                        emit(aa.right())
                    }
                }
            }
        }
    }
@OptIn(FlowPreview::class)
fun <A : Any> Flow<A>.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown(
    vararg flow: Flow<A>
): Flow<A> =
    flow {
        val flows = flow.asList() + this@ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown
        coroutineScope {
            val parentJob = coroutineContext[Job]
            val channels = flows.map {
                it.produceIn(this)
            }
            val winningItem = select {
                channels.forEachIndexed { index, ch ->
                    ch.onReceiveCatching {
                        Pair(index, it)
                    }
                }
            }
            val (index, result) = winningItem
            val winningChannel = channels[index]
            println("winningChannel $index")
            result.exceptionOrNull()?.let { throw(it) } ?: run {
                (channels - winningChannel).forEach { it.cancel() }
                println("cancelled channels")
                println("result $result")
                println("result item or null ${result.getOrNull()}")
                result.getOrNull()?.let {
                    println("not expecting item $it")
                    emit(it)
                } ?: suspend { println("emitting emptyFlow"); emitAll(emptyFlow()) }()
            }
            for (item in winningChannel) {
                emit(item)
            }
            parentJob!!.children.map { it.isCompleted }.toList().let {
                println("completed jobs $it")
            }
            println("end of coroutineScope")
        }
        println("out of coroutineScope")
    }

// TODO How about allowing siphoning off the results of failing or losing flows into
//   a "gutter" flow?  Or having one such flow for failures and another for losing flows.

object End

@OptIn(FlowPreview::class)
fun <A> Flow<A>.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures(
    vararg others: Flow<A>
): Flow<Either<Throwable, A>> =
    flow {
        coroutineScope {
            (listOf(this@ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures) + others).foldIndexed(
                Pair(emptyList<Job>(), emptyList<Flow<Pair<Int, Either<Throwable, Either<End, A>>>>>())
            ) { index, acc, curr ->
                val job = Job()
                val flow: Flow<Pair<Int, Either<Throwable, Either<End, A>>>> = curr.map {
                    Pair(index, it.right().right() as Either<Throwable, Either<End, A>>)
                }
                    .catch { emit(Pair(index, it.left())) }
                    .onCompletion {
                        println("onCompletion $index")
                        emit(Pair(index, End.left().right()))
                    }
                    .onEach { println("source flow $it") }
                    // We need shareIn in order to cancel losing flows.
                    .shareIn(CoroutineScope(job), SharingStarted.WhileSubscribed())
                Pair(acc.first + job, acc.second + flow)
            }.let { (jobs, flows) ->
                var winnerIndex: Int? = null
                flows.asFlow()
                    .flattenMerge(concurrency = flows.size)
                    .collect { (index, item) ->
                        println("collecting $item")
                        if (winnerIndex == null) {
                            item.fold(ifLeft = {
                                val uncancelledJobs = jobs.count { !it.isCancelled }
                                if (uncancelledJobs == 1) {
                                    winnerIndex = index
                                } else jobs[index].cancel()
                            }, ifRight = {
                                if (it.isRight()) {
                                    winnerIndex = index
                                    jobs.forEachIndexed { i, job ->
                                        if (!job.isCancelled && i != index) {
                                            job.cancel()
                                        }
                                    }
                                }
                            })
                        }
                        println("emitting $item")
                        if ((index == winnerIndex && item == End.left().right()) ||
                            item != End.left().right()) {
                            emit(item)
                        }
                    }
            }
        }
    }.takeWhile {
        it != End.left().right()
    }.flatMapConcat {
        it.fold(
            ifLeft = { t -> flowOf(t.left()) },
            ifRight = { e ->
                e.fold (
                    ifLeft = { emptyFlow() },
                    ifRight = { a -> flowOf(a.right()) }
                )
            })
    }

// endregion

//suspend fun <A> Flow<A>.ambUsingSelectAndIgnoringExceptions(vararg others: Flow<A>): Flow<A> =
//    flow {
//        try {
//            supervisorScope {
//                val flows = listOf(this@ambUsingSelectAndIgnoringExceptions) + others
//                val channels: List<ReceiveChannel<A>> =
//                    flows.map { flow ->
//                        produce {
//                            flow.onCompletion {
//                                println("completed $flow ${flows.indexOf(flow)} with exception $it")
//                            }.collect {
//                                send(it)
//                            }
//                        }
//                    }
//                val first = select<Pair<A?, ReceiveChannel<A>>> {
//                    for (channel in channels) {
//                        channel.onReceiveCatching {
//                            Pair(it.getOrNull(), channel)
//                        }
//                    }
//                }
//                val (a, winner) = first
//                channels.filterNot { it == winner }
//                    .forEach {
//                        it.cancel()
//                    }
//                a?.let {
//                    emit(a)
//                    for (aa in winner) {
//                        emit(aa)
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            println("exception: $e")
//            throw(e)
//        }
//    }

//suspend fun <A> Flow<A>.ambUsingSharedFlow(vararg others: Flow<A>): Flow<A> {
//    return channelFlow {
//        lateinit var jobs: List<Job>
//        jobs = (listOf(this@ambUsingSharedFlow) + others).mapIndexed { index1, flow ->
//            launch(start = CoroutineStart.LAZY) {
//                val sharedFlow = flowOf(flow).shareIn(this, SharingStarted.Lazily, 1)
//                sharedFlow.flatMapLatest { it }.first()
//                jobs.filterIndexed { index2, _ -> index1 != index2 }
//                    .forEach { it.cancel() }
//                sharedFlow.flatMapLatest { it }.collect { a ->
//                    send(a)
//                }
//            }
//        }
//        jobs.forEach {
//            it.start()
//        }
//    }
//}

//suspend fun <A> Flow<A>.amb(vararg others: Flow<A>): Flow<A> {
//    return channelFlow {
//        lateinit var jobs: List<Job>
//        jobs = (listOf(this@amb) + others).mapIndexed { index1, flow ->
//            launch(start = CoroutineStart.LAZY) {
////                val sharedFlow = flow.shareIn(this, SharingStarted.Eagerly)
//                flow.onStart {
//                    jobs.filterIndexed { index2, _ -> index1 != index2 }
//                        .forEach { it.cancel() }
//                }
//                flow.collectIndexed { index, a ->
////                    if (index == 0) {
////                        jobs.filterIndexed { index2, _ -> index1 != index2 }
////                            .forEach { it.cancel() }
////                    }
//                    send(a)
//                }
//            }
//        }
//        jobs.forEach {
//            it.start()
//        }
//    }
//}
//
//@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
//@OptIn(ExperimentalCoroutinesApi::class)
//suspend fun <A> Flow<A>.amb2(other: Flow<A>): Flow<A> {
//    return flow {
//        emit(this@amb2)
//        emit(other)
//    }.flatMapLatest { it }
//}
