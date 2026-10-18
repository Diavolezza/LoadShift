package de.wannerit.loadshift.core

/*
 * E4, "Data diet" (docs/CONCEPT.md 9.2): the shop's pages are heavy – large images in one size for every screen, a
 * product video that plays by itself, 100 products per list, six tracking tags. Every byte travels through the
 * network and is processed on the visitor's device; this scenario counts both, not only the data center. Energy per
 * GB after the Sustainable Web Design Model, version 4 (operational part: networks 0.059 kWh/GB, user devices
 * 0.080 kWh/GB [S28]). The page must stay fast: Largest Contentful Paint at most 2.5 s, the "good" threshold [S30].
 */

enum class ImageFormat(val label: String, val bytes: Int, val heroTenths: Int) {
    JPEG("JPEG", 1_000, 9),
    WEBP("WebP", 700, 7),
    AVIF("AVIF, converted once", 500, 5),
    /** The same bytes, but converted on every request: much more work for the servers, and later. */
    AVIF_LIVE("AVIF, converted on every request", 500, 5),
}

enum class LazyLoading(val label: String) { OFF("off"), BELOW_FOLD("below the fold"), ALL("all images") }

enum class Tracking(val label: String, val kilobytes: Int, val tenths: Int) {
    SIX_TAGS("six tracking tags", 1_100, 7),
    ONE_TAG("one analytics tag", 150, 2),
    SERVER("counting on the server", 0, 0),
}

/** The player's settings for the pages. */
data class PageConfig(
    val format: ImageFormat = ImageFormat.JPEG,
    /** Image sizes for the screen (srcset) instead of one large size for every screen. */
    val responsive: Boolean = false,
    val lazy: LazyLoading = LazyLoading.OFF,
    /** The product video plays by itself (else on click). */
    val autoplay: Boolean = true,
    /** Products per list page: 100 (else 24 and a "more" button). */
    val longLists: Boolean = true,
    val tracking: Tracking = Tracking.SIX_TAGS,
)

/** What a visit transfers, by part, KB. */
data class PageWeight(val images: Int, val video: Int, val code: Int, val tracking: Int) {
    val total: Int get() = images + video + code + tracking
}

data class WebModel(
    /** Requests per visit (pages, images and scripts from the shop's servers). */
    val requestsPerVisit: Int = 10,
    /** Network and device energy per GB, Wh (operational, [S28]). */
    val networkWhPerGb: Int = 59,
    val deviceWhPerGb: Int = 80,
    /** LCP limit, tenths of a second. */
    val lcpLimit: Int = 25,
) {
    fun weight(c: PageConfig): PageWeight {
        val raw = 3_000 + if (c.longLists) 1_200 else 300
        var images = raw.toDouble() * c.format.bytes / 1_000
        if (c.responsive) images *= 0.55
        images *= when (c.lazy) { LazyLoading.OFF -> 1.0; LazyLoading.BELOW_FOLD -> 0.6; LazyLoading.ALL -> 0.55 }
        val video = if (c.autoplay) 6_000 else 300
        return PageWeight(images.toInt(), video, 900, c.tracking.kilobytes)
    }

    /** Largest Contentful Paint at the 75th percentile, tenths of a second (game values). */
    fun lcp(c: PageConfig): Int {
        var hero = c.format.heroTenths.toDouble()
        if (c.responsive) hero *= 0.6
        return 10 + hero.toInt() +
            (if (c.lazy == LazyLoading.ALL) 13 else 0) +
            (if (c.format == ImageFormat.AVIF_LIVE) 15 else 0) +
            c.tracking.tenths +
            (if (c.longLists) 3 else 0) +
            (if (c.autoplay) 3 else 0)
    }

    /** Work per request on the shop's servers, per mille of the reference pages. */
    fun work(c: PageConfig): Int {
        var w = 1_000.0
        if (c.format == ImageFormat.AVIF_LIVE) w *= 1.8
        if (!c.longLists) w *= 0.85
        if (c.tracking == Tracking.SERVER) w *= 1.03
        return w.toInt()
    }
}

/** E4: what the visits drew outside the data center in a tick, W. */
data class WebRecord(val networkWatts: Int, val deviceWatts: Int)
