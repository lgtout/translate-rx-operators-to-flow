package com.lagostout.turbine

import app.cash.turbine.FlowTurbine

suspend fun <A> FlowTurbine<A>.awaitItems(count: Int): List<A> = (1..count).map {
    val item = awaitItem()
    println(item)
    item
}

