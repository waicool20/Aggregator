/*
 * GPLv3 License
 *  Copyright (c) aggregator by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.aggregator

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

inline fun <T> Iterable<T>.parallelForEach(pool: CoroutineContext = CommonPool, crossinline action: (T) -> Unit): Unit {
    val jobs = mutableListOf<Job>()
    for (element in this) {
        launch(pool) {
            action(element)
        }.let(jobs::add)
    }
    runBlocking {
        jobs.forEach { it.join() }
    }
}

inline fun <T, R> Iterable<T>.parallelMap(pool: CoroutineContext = CommonPool, crossinline transform: (T) -> (R)): List<R> =
        parallelMapTo(pool, mutableListOf<R>(), transform)


inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.parallelMapTo(pool: CoroutineContext = CommonPool, destination: C, crossinline transform: (T) -> R): C {
    val jobs = mutableListOf<Deferred<R>>()
    for (item in this@parallelMapTo) {
        async(pool) {
            transform(item)
        }.let(jobs::add)
    }
    runBlocking {
        jobs.mapTo(destination, { it.await() })
    }
    return destination
}

inline fun <T, R> Iterable<T>.parallelFlatMap(pool: CoroutineContext = CommonPool, crossinline transform: (T) -> Iterable<R>): List<R> =
        parallelFlatMapTo(pool, mutableListOf<R>(), transform)


inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.parallelFlatMapTo(pool: CoroutineContext = CommonPool, destination: C, crossinline transform: (T) -> Iterable<R>): C {
    val jobs = mutableListOf<Deferred<Iterable<R>>>()
    for (element in this) {
        async(pool) {
            transform(element)
        }.let(jobs::add)
    }
    runBlocking {
        jobs.flatMapTo(destination, { it.await() })
    }
    return destination
}

inline fun <T, C : Iterable<T>> C.parallelOnEach(pool: CoroutineContext = CommonPool, crossinline action: (T) -> Unit): C {
    val jobs = mutableListOf<Job>()
    for (element in this) {
        launch(pool) {
            action(element)
        }.let(jobs::add)
    }
    runBlocking {
        jobs.forEach { it.join() }
    }
    return this
}
