package de.wannerit.loadshift.core

/*
 * Scenario O9, eco mode for video streaming (docs/CONCEPT.md 5.4, 9.1): viewers watch on phones, laptops and TVs.
 * The work in the data center grows with the bitrate (encoding, packaging, delivery). The player sets the highest
 * quality per device and an eco mode that lowers the quality while the grid is dirty – demand shaping. The viewers
 * notice the quality differently on each device; their mean rating must stay above a target.
 */

/** Video quality and its work, per mille of 4K. */
enum class Quality(val label: String, val work: Int) { UHD("4K", 1_000), HD("HD", 250), SD("SD", 100) }

/** A kind of device: its share of the viewers and how its viewers rate 4K, HD and SD, per mille. */
data class Device(val title: String, val share: Int, val rating: List<Int>)

/** The player's settings: the highest quality per device and the eco mode. */
data class StreamingConfig(
    val maxQuality: List<Quality> = emptyList(),
    val eco: Boolean = false,
    /** Eco mode while the grid is above this, g/kWh. */
    val ecoAbove: Int = 600,
    /** The highest quality in eco mode. */
    val ecoQuality: Quality = Quality.HD,
)

data class StreamingModel(
    val devices: List<Device>,
    /** Work per stream that does not depend on the quality (sessions, rights, logs), per mille of the work at 4K. */
    val fixedWork: Int = 150,
    /** The viewers' mean rating the service must keep, per mille. */
    val ratingTarget: Int,
) {
    fun quality(config: StreamingConfig, device: Int, gramsPerKWh: Int): Quality {
        val max = config.maxQuality.getOrElse(device) { Quality.UHD }
        return if (config.eco && gramsPerKWh > config.ecoAbove && config.ecoQuality.ordinal > max.ordinal) config.ecoQuality else max
    }

    fun ecoActive(config: StreamingConfig, gramsPerKWh: Int): Boolean = config.eco && gramsPerKWh > config.ecoAbove

    /** Work per stream, per mille of the work when everybody watches in 4K. */
    fun work(config: StreamingConfig, gramsPerKWh: Int): Int =
        fixedWork + (1_000 - fixedWork) * devices.indices.sumOf { devices[it].share * quality(config, it, gramsPerKWh).work } / 1_000_000

    /** The viewers' mean rating, per mille. */
    fun rating(config: StreamingConfig, gramsPerKWh: Int): Int =
        devices.indices.sumOf { devices[it].share * devices[it].rating[quality(config, it, gramsPerKWh).ordinal] } / 1_000

    companion object {
        val ECO_RANGE = 400..800
    }
}
