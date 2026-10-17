package de.wannerit.loadshift.core

/** Servers (docs/CONCEPT.md, section 6). Three kinds, two generations: one entry per kind and generation. */
enum class ServerKind(
    val title: String,
    /** Maximum power, W (SPECpower about 303 W per socket [S9]; DGX H100 up to 10.2 kW [S12]). */
    val maxPower: Int,
    /** Idle power as a share of the maximum, per mille (SPECpower 2024: 25.7 % [S9]; older servers more). */
    val idlePermille: Int,
    /**
     * Work units per tick at full load for interactive and streaming work (game value: "current" does about twice
     * the work per kWh of "old"; ARM about a fifth more than the current CPU server [S11]). GPU servers do none.
     */
    val capacity: Int,
    /** Embodied emissions in kg CO₂e (Dell R760 LCA [S10], HGX H100 PCF [S13], game values in between). */
    val embodiedKilograms: Int,
    /** RAM for virtual machines, GB (game value). */
    val ramGb: Int,
) {
    CPU_OLD("CPU server, old", 600, 350, 10, 500, 128),
    CPU_CURRENT("CPU server", 600, 260, 20, 550, 256),
    ARM_CURRENT("ARM server", 300, 200, 12, 450, 256),
    GPU_OLD("GPU server, old", 6_400, 150, 0, 1_450, 1_024),
    GPU_CURRENT("GPU server", 10_200, 150, 0, 1_800, 2_048);

    val idlePower: Int get() = maxPower * idlePermille / 1_000

    /** Embodied emissions booked per tick in service, µg (TE / (6 years × 96 ticks per day)). */
    val embodiedPerTick: Long
        get() = embodiedKilograms * 1_000_000_000L / (Units.LIFESPAN_DAYS * GameTime.TICKS_PER_DAY)

    /** Serves interactive and streaming work. */
    val servesInteractive: Boolean get() = capacity > 0

    /** Runs batch jobs (CPU servers). */
    val servesBatch: Boolean get() = this == CPU_OLD || this == CPU_CURRENT

    /** Work per kW at full load, used to pick the most efficient servers first. */
    val efficiency: Int get() = capacity * 1_000_000 / maxPower

    companion object {
        /** Ticks from switching on until the server takes work: 30 minutes (game value). */
        const val BOOT_TICKS = 2
    }
}

sealed interface ServerState {
    data object Off : ServerState

    /** Switched on, takes work from the given tick. */
    data class Booting(val readyAt: Int) : ServerState
    data object On : ServerState

    val isOn: Boolean get() = this == On
    val isPowered: Boolean get() = this != Off
}

data class Server(
    val id: Int,
    val kind: ServerKind,
    val state: ServerState = ServerState.On,
    /** Share of its capacity used in the last tick, per mille. */
    val utilization: Int = 0,
    /** The batch job the server works for, if any. */
    val job: Int? = null,
    /** Age when it was installed, days, and the tick it was installed at (O7: old servers and their end of life). */
    val ageDays: Int = 0,
    val installedAt: Int = 0,
) {
    /** Age at a tick, real days; [scale]: real days per game day. */
    fun ageAt(tick: Int, scale: Long = 1): Int = ageDays + ((tick - installedAt) * scale / GameTime.TICKS_PER_DAY).toInt()

    /**
     * Power drawn at the given utilization, W: idle plus the dynamic part in proportion to the load. A booting
     * server draws idle power, a switched-off one nothing.
     */
    fun power(utilization: Int = this.utilization): Int = when (state) {
        ServerState.Off -> 0
        is ServerState.Booting -> kind.idlePower
        ServerState.On -> kind.idlePower + (kind.maxPower - kind.idlePower) * utilization / 1_000
    }
}
