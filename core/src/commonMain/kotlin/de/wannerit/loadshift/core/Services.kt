package de.wannerit.loadshift.core

import kotlin.math.ceil

/*
 * E7, "Chatty services" (docs/CONCEPT.md 9.2): a logistics system – customers track their deliveries, dispatchers
 * plan the trucks' routes. Four microservices (tracking, routes, customers, trucks) call each other synchronously with
 * REST and JSON, and the routes service asks the GPS provider and the map service (traffic) on every request. The
 * levers follow the usual architecture options: a modular monolith instead of microservices, a binary protocol
 * (gRPC with Protobuf: about half the bytes of JSON [S33]), events with a local copy of the master data instead of
 * synchronous calls, a cache for the external calls, and serverless functions for services that are rarely called.
 */

enum class ServiceCut(val label: String) { MICROSERVICES("microservices"), MODULITH("modular monolith") }

enum class Protocol(val label: String, val hopMs: Double, val latencyMs: Int) { REST_JSON("REST with JSON", 4.0, 12), GRPC("gRPC with Protobuf", 1.5, 5) }

enum class MasterData(val label: String) { SYNC("synchronous calls"), EVENTS("events and a local copy") }

data class ServiceConfig(
    val cut: ServiceCut = ServiceCut.MICROSERVICES,
    val protocol: Protocol = Protocol.REST_JSON,
    val masterData: MasterData = MasterData.SYNC,
    /** How long a GPS position and traffic information are reused, seconds (0: every request asks). */
    val gpsTtl: Int = 0,
    val mapTtl: Int = 0,
    /** The customer and truck services as serverless functions (microservices only). */
    val serverless: Boolean = false,
)

data class ServiceModel(
    /** Tracking requests per second at the evening peak. */
    val peakRate: Int = 15_000,
    val trucks: Int = 400,
    /** Regions with their own traffic information. */
    val regions: Int = 60,
    /** Mean response time of the tracking the customers accept, ms. */
    val latencyLimit: Int = 250,
    /** A truck's position may be at most this old for the customers, the traffic information for the dispatchers, s. */
    val gpsLimit: Int = 60,
    val mapLimit: Int = 300,
) {
    /** Remote hops per tracking request (tracking → routes; routes → customers and trucks when synchronous). */
    fun remoteHops(c: ServiceConfig): Int = if (c.cut == ServiceCut.MODULITH) 0 else 1 + if (c.masterData == MasterData.SYNC) 2 else 0

    /** Calls to serverless functions per tracking request. */
    fun functionCalls(c: ServiceConfig): Int = if (c.cut == ServiceCut.MICROSERVICES && c.serverless && c.masterData == MasterData.SYNC) 2 else 0

    /** Share of the external calls that go out (the rest come from the cache), per request rate. */
    fun missShare(ttl: Int, distinct: Int, rate: Double): Double = if (ttl == 0 || rate <= 0.0) 1.0 else minOf(1.0, distinct.toDouble() / ttl / rate)

    /** CPU work per tracking request, ms. */
    fun cpuPerRequest(c: ServiceConfig, rate: Double): Double {
        val own = 10.0
        val hops = remoteHops(c) * c.protocol.hopMs + (if (c.cut == ServiceCut.MODULITH) 0.3 else 0.0)
        val external = 2 * 0.2 + 2.8 * (missShare(c.gpsTtl, trucks, rate) + missShare(c.mapTtl, regions, rate))
        val functions = functionCalls(c) * 6.0
        return own + hops + external + functions
    }

    /** Instances that run all the time (their runtime, sidecar and health checks cost CPU even without load). */
    fun replicas(c: ServiceConfig): Int = when (c.cut) {
        ServiceCut.MODULITH -> 2
        ServiceCut.MICROSERVICES -> 2 + 2 + (if (c.serverless) 0 else 4) + (if (c.masterData == MasterData.EVENTS) 2 else 0)
    } + (if (c.gpsTtl > 0 || c.mapTtl > 0) 1 else 0)

    /** CPU work per second at a request rate, ms (32 cores × 1,000 ms per server). */
    fun cpuPerSecond(c: ServiceConfig, rate: Double): Double = rate * cpuPerRequest(c, rate) + replicas(c) * REPLICA_MS

    /** Servers needed: the work at 70 % utilization, and at least one server per four replicas. */
    fun servers(c: ServiceConfig, rate: Double): Int =
        maxOf(ceil(replicas(c) / 4.0).toInt(), ceil(cpuPerSecond(c, rate) / (SERVER_MS * 0.7)).toInt())

    /** Mean response time of the tracking, ms. */
    fun latency(c: ServiceConfig, rate: Double): Int {
        val external = 120 * missShare(c.gpsTtl, trucks, rate) + 150 * missShare(c.mapTtl, regions, rate)
        return (30 + remoteHops(c) * c.protocol.latencyMs + functionCalls(c) * 25 + external).toInt()
    }

    /** Freshness kept: GPS positions and traffic information young enough. */
    fun fresh(c: ServiceConfig): Boolean = c.gpsTtl <= gpsLimit && c.mapTtl <= mapLimit

    companion object {
        const val SERVER_MS = 32_000.0
        const val REPLICA_MS = 600.0
        val GPS_TTLS = listOf(0, 10, 60, 300)
        val MAP_TTLS = listOf(0, 60, 300, 3_600)
    }
}

/** E7: servers on and the CPU work, and the share of the work in communication. */
data class ServiceRecord(val servers: Int, val cpuMs: Int, val overheadMs: Int)
