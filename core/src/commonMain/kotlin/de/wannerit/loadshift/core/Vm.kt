package de.wannerit.loadshift.core

/*
 * Virtual machines for the consolidation scenario (docs/CONCEPT.md 9.1, scenario 5): each VM needs a fixed amount of
 * RAM and CPU work that follows its own daily profile. VMs are placed on hosts; a host whose VMs together need more
 * than it has makes requests slow (CPU) – or the VMs do not fit at all (RAM).
 */

/** Kinds of VM with their RAM, their CPU peak (work units per tick) and their daily profile (per mille of the peak). */
enum class VmKind(val title: String, val short: String, val ramGb: Int, val peak: Int, val profile: List<Int>) {
    FRONT_END("Shop front end", "Shop", 32, 7, InteractiveLoad.DAILY_CURVE),
    BATCH_WORKER("Batch worker", "Batch", 48, 8, listOf(
        1_000, 1_000, 1_000, 1_000, 1_000, 900, 500, 200, 100, 100, 100, 100,
        100, 100, 100, 100, 100, 100, 100, 150, 300, 600, 900, 1_000,
    )),
    REPORTING("Reporting", "Report", 48, 8, listOf(
        100, 100, 100, 100, 100, 150, 400, 800, 1_000, 1_000, 900, 600,
        300, 200, 150, 150, 150, 150, 100, 100, 100, 100, 100, 100,
    )),
    DATABASE("Database replica", "DB", 128, 6, InteractiveLoad.DAILY_CURVE.map { 500 + it / 2 }),
    CACHE("Cache", "Cache", 64, 2, List(24) { 1_000 });

    /** Expected CPU work at a tick, in thousandths of a work unit (profile linear between the full hours). */
    fun expectedMilli(tick: Int): Int {
        val perHour = GameTime.TICKS_PER_HOUR
        val inDay = tick % GameTime.TICKS_PER_DAY
        val hour = inDay / perHour
        val r = inDay % perHour
        val share = (profile[hour] * (perHour - r) + profile[(hour + 1) % 24] * r) / perHour
        return peak * share
    }
}

/** Placing VMs on hosts: checks and the classic heuristics (bin packing). */
object Packing {
    /** Share of a host's CPU up to which requests stay fast, per mille (the same as for interactive work). */
    const val SAFE = InteractiveLoad.SLOW_ABOVE

    fun ramUsed(vms: List<VmKind>, placement: List<Int>, host: Int): Int =
        vms.indices.filter { placement[it] == host }.sumOf { vms[it].ramGb }

    /** Highest expected CPU work of a host over the day, thousandths of a work unit. */
    fun expectedPeakMilli(vms: List<VmKind>, placement: List<Int>, host: Int): Int {
        val on = vms.indices.filter { placement[it] == host }
        if (on.isEmpty()) return 0
        return (0 until GameTime.TICKS_PER_DAY).maxOf { t -> on.sumOf { vms[it].expectedMilli(t) } }
    }

    /** Every VM one after another round the hosts – what the previous team did ("spread"). */
    fun spread(vms: List<VmKind>, hosts: Int): List<Int> = vms.indices.map { it % hosts }

    /** How a heuristic decides whether a VM still fits on a host. */
    enum class Check { RAM_ONLY, PEAK_SUM, PROFILES }

    /**
     * First Fit Decreasing: VMs sorted by [key] (largest first), each onto the first host where it fits by [check]
     * (CPU up to [limit] per mille of the host); a new host when it fits nowhere. When all [fleet] hosts are in use,
     * the VM goes to the host with the most free RAM. Returns the host per VM.
     */
    fun firstFitDecreasing(
        vms: List<VmKind>, host: ServerKind, fleet: Int, check: Check, limit: Int = SAFE, key: (VmKind) -> Int,
    ): List<Int> {
        val placement = MutableList(vms.size) { -1 }
        var hosts = 0
        val safeMilli = host.capacity * limit
        for (i in vms.indices.sortedByDescending { key(vms[it]) }) {
            val vm = vms[i]
            var chosen = -1
            for (h in 0 until hosts) {
                if (ramUsed(vms, placement, h) + vm.ramGb > host.ramGb) continue
                val fits = when (check) {
                    Check.RAM_ONLY -> true
                    Check.PEAK_SUM -> vms.indices.filter { placement[it] == h }.sumOf { vms[it].peak * 1_000 } + vm.peak * 1_000 <= safeMilli
                    Check.PROFILES -> (0 until GameTime.TICKS_PER_DAY).all { t ->
                        vms.indices.filter { placement[it] == h }.sumOf { vms[it].expectedMilli(t) } + vm.expectedMilli(t) <= safeMilli
                    }
                }
                if (fits) { chosen = h; break }
            }
            if (chosen < 0) {
                chosen = if (hosts < fleet) hosts++ else (0 until hosts).maxBy { host.ramGb - ramUsed(vms, placement, it) }
            }
            placement[i] = chosen
        }
        return placement
    }
}
