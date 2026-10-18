package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Hosting
import de.wannerit.loadshift.core.Inventory
import de.wannerit.loadshift.core.OnPremKey
import de.wannerit.loadshift.core.PortfolioResult
import de.wannerit.loadshift.core.Scenario
import de.wannerit.loadshift.core.Source

/** State of a portfolio scenario: the data bought, the plan, and whether the year is done. */
class PortfolioController(val scenario: Scenario) {
    val model = scenario.portfolio!!
    var inventory by mutableStateOf(Inventory())
        private set
    var plan by mutableStateOf(emptySet<Int>())
        private set
    var finished by mutableStateOf(false)
        private set

    val dataCost: Int get() = model.inventoryCost(inventory)
    val planCost: Int get() = model.planCost(plan)
    val left: Int get() = model.budget - dataCost - planCost
    val inventoryKg: Long get() = model.inventoryKg(inventory)
    val expectedSaving: Long get() = plan.sumOf { model.expectedSaving(it, inventory) }
    val result: PortfolioResult get() = model.result(inventory, plan)
    val stars: Int get() = model.stars(result, scenario.goals)

    /** A key for the own data center: once bought it stays bought, switching back costs nothing. */
    fun chooseKey(key: OnPremKey) {
        if (finished) return
        if (key in inventory.bought) inventory = inventory.copy(key = key)
        else if (key.costEuro <= left) inventory = inventory.copy(key = key, bought = inventory.bought + key)
    }

    fun recalculate() {
        if (!finished && !inventory.recalculated && model.recalculateCost <= left) inventory = inventory.copy(recalculated = true)
    }

    fun ask(i: Int) {
        if (!finished && i !in inventory.asked && model.askCost <= left) inventory = inventory.copy(asked = inventory.asked + i)
    }

    fun canAfford(i: Int) = i in plan || model.apps[i].measure.costEuro <= left

    fun toggle(i: Int) {
        if (finished) return
        if (i in plan) plan = plan - i else if (canAfford(i)) plan = plan + i
    }

    fun finish() {
        finished = true
        Progress.record(stars, scenario.number)
    }

    fun startOver() {
        inventory = Inventory()
        plan = emptySet()
        finished = false
    }

    /** The same budget with other data, each planned the careful way: the measures with the most expected saving per euro. */
    fun whatIf(): List<Pair<String, PortfolioResult>> {
        val all = model.apps.indices.filter { model.apps[it].hosting == Hosting.SAAS }.toSet()
        fun inv(key: OnPremKey, cloud: Boolean, asked: Set<Int>) = Inventory(key, setOf(OnPremKey.SERVERS, key), cloud, asked)
        return listOf(
            t("Only the free data") to inv(OnPremKey.SERVERS, false, emptySet()),
            t("Every source of data bought, the cloud still market-based") to inv(OnPremKey.POWER, false, all),
            t("Cloud recalculated, the rest free") to inv(OnPremKey.SERVERS, true, emptySet()),
            t("CPU time and cloud recalculated") to inv(OnPremKey.CPU_TIME, true, emptySet()),
            t("Measured power and cloud recalculated") to inv(OnPremKey.POWER, true, emptySet()),
            t("Every source of data bought") to inv(OnPremKey.POWER, true, all),
        ).map { (label, i) -> label to model.result(i, model.greedyPlan(i)) }
    }
}

private fun tonnes(kg: Long): String = Format.number(kg / 1_000.0, if (kg in 1..9_999) 1 else 0)
private fun euros(euro: Int): String = Format.number(euro.toDouble()) + " €"

fun hostingTitle(h: Hosting): String = when (h) {
    Hosting.ON_PREM -> t("Own data center")
    Hosting.CLOUD -> t("Cloud")
    Hosting.SAAS -> t("SaaS")
}

/** P1: three columns of applications, the data sources and the plan on the right. */
@Composable
fun PortfolioScreen(game: PortfolioController, onExit: () -> Unit, onOverview: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background)) {
        val panelWidth = (maxWidth * 0.3f).coerceIn(300.dp, 380.dp)
        Column(Modifier.fillMaxSize()) {
            PortfolioTopBar(game, onExit)
            PortfolioGoals(game)
            HDivider()
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Row(Modifier.weight(1f).fillMaxHeight().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (h in Hosting.entries) HostingColumn(game, h, Modifier.weight(1f).fillMaxHeight())
                }
                VDivider()
                PortfolioPanel(game, Modifier.width(panelWidth).fillMaxHeight())
            }
        }
        if (game.finished) PortfolioEnd(game, onExit, onOverview)
    }
}

@Composable
private fun PortfolioTopBar(game: PortfolioController, onExit: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Txt("‹", Type.style(34, FontWeight.SemiBold), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onExit).padding(horizontal = 6.dp))
            Column {
                Txt(t("Scenario %@", game.scenario.shownLabel), Type.caption)
                Txt(t(game.scenario.title), Type.style(17, FontWeight.SemiBold), maxLines = 1)
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(t("Year 1"), Type.caption)
            Txt(if (game.finished) t("Plan carried out") else t("Inventory and plan"), Type.style(24, FontWeight.SemiBold))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Metric(t("Your inventory"), t("%@ t CO₂", tonnes(game.inventoryKg)))
            Metric(t("Expected by your data"), t("−%@ t", tonnes(game.expectedSaving)))
            Metric(t("Budget left"), euros(game.left), if (game.left < 0) Palette.red else Palette.ink)
        }
    }
}

@Composable
private fun PortfolioGoals(game: PortfolioController) {
    val scenario = game.scenario
    val r = game.result
    val met = game.model.goalsMet(r, scenario.goals)
    Row(
        Modifier.fillMaxWidth().background(Palette.panel).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Txt(t("Goals"), Type.style(13, FontWeight.SemiBold, Palette.secondary))
        GoalItem(met.rules, game.finished, t("Inventory within ±%@ of the real footprint", Format.wholePercent(game.model.inventoryLimit)))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(2, 13.dp)
            Txt(t("CO₂ %@", Format.changePermille(-scenario.goals.reduction.toLong())), Type.callout)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StarLabel(3, 13.dp)
            Txt(specialLong(scenario.goals.special), Type.callout)
        }
        Spacer(Modifier.weight(1f))
        StartOverChip { game.startOver() }
    }
}

@Composable
private fun HostingColumn(game: PortfolioController, hosting: Hosting, modifier: Modifier) {
    val model = game.model
    val indices = model.apps.indices.filter { model.apps[it].hosting == hosting }
    val sum = indices.sumOf { model.estimate(it, game.inventory) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(hostingTitle(hosting), Type.heading, Modifier.weight(1f))
            Txt(t("%@ t", tonnes(sum)), Type.style(17, FontWeight.SemiBold))
        }
        Txt(when (hosting) {
            Hosting.ON_PREM -> t("Stuttgart: the meter shows %@ MWh a year, cooling included; the grid's mean %lld g/kWh.", Format.number(model.meterKWh / 1_000.0), model.gridGrams)
            Hosting.CLOUD -> t("One provider; it buys green certificates for all its regions.")
            Hosting.SAAS -> t("Known: the invoices. Spend-based: %@ kg CO₂e per 1,000 €.", Format.number(model.spendGrams.toDouble()))
        }, Type.caption)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in indices) AppCard(game, i)
        }
    }
}

/** Largest number a bar shows at full width, kg. */
private const val BAR_KG = 150_000.0

@Composable
private fun AppCard(game: PortfolioController, i: Int) {
    val model = game.model
    val app = model.apps[i]
    val source = model.source(i, game.inventory)
    val estimate = model.estimate(i, game.inventory)
    val inPlan = i in game.plan
    Column(
        Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(12.dp))
            .border(if (inPlan) 1.5.dp else 0.dp, if (inPlan) Palette.accent else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Txt(t(app.name), Type.style(15, FontWeight.SemiBold))
        Txt(t("%@ a year", euros(app.costPerYear)) + " · " + t(app.description), Type.caption)
        // The inventory's number as a bar; after the year the real one below it.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Bar(estimate, if (source.quality >= 3) Palette.accent else Palette.capacity) }
            Txt(t("≈ %@ t", tonnes(estimate)), Type.style(14, FontWeight.SemiBold), maxLines = 1)
        }
        if (game.finished) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Bar(app.trueKg, Palette.ink) }
            Txt(t("really %@ t", tonnes(app.trueKg)), Type.style(14, FontWeight.SemiBold), maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QualityDots(source.quality)
            Txt(sourceTitle(source), Type.caption)
        }
        if (app.hosting == Hosting.SAAS && i !in game.inventory.asked && !game.finished) {
            Txt(t("Ask the supplier for data · %@", euros(model.askCost)), Type.style(13, FontWeight.SemiBold, if (model.askCost <= game.left) Palette.accent else Palette.secondary),
                Modifier.clip(RoundedCornerShape(6.dp)).clickable { game.ask(i) }.padding(vertical = 2.dp))
        }
        HDivider()
        val m = app.measure
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(enabled = !game.finished && game.canAfford(i)) { game.toggle(i) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tick(inPlan)
            Column(Modifier.weight(1f)) {
                Txt(t(m.title), Type.style(14, color = if (game.canAfford(i)) Palette.ink else Palette.secondary))
                Txt(t("%@ · −%lld %%", euros(m.costEuro), m.effectPercent) +
                    if (inPlan) " · " + t("expected −%@ t", tonnes(model.expectedSaving(i, game.inventory))) else "", Type.caption)
            }
        }
    }
}

/** Quality of a number: three dots, filled as far as the data are good. */
@Composable
private fun QualityDots(quality: Int) {
    androidx.compose.foundation.Canvas(Modifier.width(26.dp).height(8.dp)) {
        val r = size.height / 2
        for (k in 0 until 3) {
            val c = androidx.compose.ui.geometry.Offset(r + k * (size.width - 2 * r) / 2, r)
            if (k < quality) drawCircle(Palette.ink, r, c)
            else drawCircle(Palette.secondary, r * 0.8f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.35f))
        }
    }
}

@Composable
private fun Bar(kg: Long, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(Palette.background, RoundedCornerShape(4.dp))) {
        val f = (kg / BAR_KG).toFloat().coerceIn(0.005f, 1f)
        Box(Modifier.fillMaxWidth(f).height(8.dp).background(color, RoundedCornerShape(4.dp)))
    }
}

fun sourceTitle(s: Source): String = when (s) {
    Source.SERVERS -> t("share by number of servers")
    Source.CPU_TIME -> t("share by CPU time")
    Source.POWER -> t("measured power")
    Source.PROVIDER -> t("provider's tool, market-based")
    Source.RECALCULATED -> t("recalculated, location-based")
    Source.SPEND -> t("spend-based")
    Source.SUPPLIER -> t("supplier's data")
    Source.DECLINED -> t("no data from the supplier – spend-based")
}

@Composable
private fun PortfolioPanel(game: PortfolioController, modifier: Modifier) {
    val model = game.model
    Column(modifier.background(Palette.panel).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Sources of data"), Type.heading)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QualityDots(3); Txt(t("good data"), Type.caption); Spacer(Modifier.width(10.dp)); QualityDots(1); Txt(t("rough"), Type.caption)
        }
        Txt(t("Data you buy stays bought. It costs from the same budget as the measures."), Type.style(15, color = Palette.secondary))
        Txt(t("Own data center: how the meter's energy is split"), Type.subheading)
        val notes = mapOf(
            OnPremKey.SERVERS to t("The servers each application has in the CMDB. Free."),
            OnPremKey.CPU_TIME to t("The CPU hours each application used, from the monitoring. %@.", euros(OnPremKey.CPU_TIME.costEuro)),
            OnPremKey.POWER to t("Metered power strips in the racks, a month long. %@.", euros(OnPremKey.POWER.costEuro)),
        )
        for (k in OnPremKey.entries) {
            val bought = k in game.inventory.bought
            RuleOption(game.inventory.key == k, { game.chooseKey(k) }, t(k.title) + if (bought && k.costEuro > 0) " · " + t("bought") else "", notes.getValue(k))
        }
        Txt(t("Cloud"), Type.subheading)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (game.inventory.recalculated) Palette.accent.copy(alpha = 0.08f) else Palette.background)
                .clickable { game.recalculate() }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Tick(game.inventory.recalculated)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(t("Recalculate from the usage data · %@", euros(model.recalculateCost)), Type.style(15, FontWeight.SemiBold))
                Txt(t("vCPU hours, storage and network of each application with the grid mix of its region, location-based. The provider's tool counts its certificates, market-based."), Type.caption)
            }
        }
        Txt(t("SaaS"), Type.subheading)
        Txt(t("Ask a supplier for data on its card, %@ each. Not every supplier answers.", euros(model.askCost)), Type.caption)
        HDivider()
        Txt(t("Your plan"), Type.heading)
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            for ((label, value) in listOf(
                t("Budget for data and measures") to euros(model.budget),
                t("Data") to (if (game.dataCost > 0) "−" else "") + euros(game.dataCost),
                t("Measures") to (if (game.planCost > 0) "−" else "") + euros(game.planCost),
                t("Left") to euros(game.left),
            )) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(value, Type.style(15, FontWeight.SemiBold))
                }
            }
        }
        Txt(t("By your inventory the measures save %@ t a year – %@ of %@ t.", tonnes(game.expectedSaving),
            Format.wholePercent(if (game.inventoryKg == 0L) 0 else (game.expectedSaving * 1_000 / game.inventoryKg).toInt()), tonnes(game.inventoryKg)), Type.callout)
        Txt(t("The measures take effect next year. The stars count what really changes."), Type.caption)
        PrimaryButton(t("Carry out the plan"), Modifier.fillMaxWidth(), enabled = !game.finished && game.plan.isNotEmpty()) { game.finish() }
    }
}

/** End of the year: stars, the inventory next to the real footprint, "what if". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortfolioEnd(game: PortfolioController, onExit: () -> Unit, onOverview: (() -> Unit)?) {
    val scenario = game.scenario
    val r = game.result
    val met = game.model.goalsMet(r, scenario.goals)
    val texts = ScenarioTexts.of(scenario)
    var closed by remember { mutableStateOf(false) }
    if (closed) {
        // The cards stay to be read; a button brings the card back.
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            PrimaryButton(t("Show the result")) { closed = false }
        }
        return
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(vertical = 40.dp, horizontal = 24.dp).widthIn(max = 640.dp).background(Palette.panel, RoundedCornerShape(20.dp))) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (!met.rules) t("Not made") else t("Scenario complete"), Type.title, Modifier.weight(1f))
                    StarRow(game.stars)
                }
                Txt("${Format.changePermille(-r.reductionPermille)} CO₂", Type.style(40, FontWeight.Bold, if (r.reductionPermille > 0) Palette.accent else Palette.red))
                Txt(t(texts.comparedWith), Type.style(17, color = Palette.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalLine(met.rules, 1, t(texts.rules))
                    GoalLine(met.reduction, 2, t("CO₂ %@ or better", Format.changePermille(-scenario.goals.reduction.toLong())))
                    GoalLine(met.special, 3, specialLong(scenario.goals.special))
                }
                Table(listOf("", t("t CO₂")), listOf(
                    listOf(t("Your inventory"), tonnes(r.inventoryKg) + "  (" + Format.changePermille(r.inventoryError) + ")"),
                    listOf(t("Real footprint, location-based"), tonnes(r.trueKg)),
                    listOf(t("Next year, with your measures"), tonnes(r.afterKg)),
                    listOf(t("Spent on data (€)"), Format.number(r.inventoryCost.toDouble())),
                    listOf(t("Spent on measures (€)"), Format.number(r.planCost.toDouble())),
                ), firstWidth = 0.6f)
                Txt(t("What you did is called %@. %@", texts.term, t(texts.termText)), Type.style(15, color = Palette.secondary))
                Txt(t(texts.remark), Type.style(15, color = Palette.secondary))
                PortfolioWhatIf(game)
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(t("Play again")) { game.startOver() }
                SecondaryButton(t("Look at the cards")) { closed = true }
                SecondaryButton(t("To the briefing"), onClick = onExit)
                if (onOverview != null) SecondaryButton(t("To the overview"), onClick = onOverview)
            }
        }
    }
}

@Composable
private fun PortfolioWhatIf(game: PortfolioController) {
    val alternatives = remember(game.finished) { game.whatIf() }
    val best = maxOf(Progress.stars(game.scenario.number), game.stars)
    val goals = game.scenario.goals
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Txt(t("What if …"), Type.subheading)
        Txt(t("The same budget with other data, each time the measures with the most expected saving per euro:"), Type.style(15, color = Palette.secondary))
        Spacer(Modifier.height(8.dp))
        Row {
            Spacer(Modifier.weight(0.5f))
            Txt(t("Inventory"), Type.caption.copy(textAlign = TextAlign.End), Modifier.weight(0.17f))
            Txt(t("CO₂"), Type.caption.copy(textAlign = TextAlign.End), Modifier.weight(0.17f))
            Spacer(Modifier.weight(0.16f))
        }
        var hidden = 0
        for ((label, r) in alternatives) {
            val stars = game.model.stars(r, goals)
            val secret = stars > best
            if (secret) hidden++
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (secret) Txt(t("A setting you have not found yet"), Type.style(15, color = Palette.secondary).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), Modifier.weight(0.5f))
                else Txt(label, Type.callout, Modifier.weight(0.5f))
                Txt(Format.changePermille(r.inventoryError), Type.style(15, color = if (kotlin.math.abs(r.inventoryError) <= game.model.inventoryLimit) Palette.ink else Palette.red).copy(textAlign = TextAlign.End), Modifier.weight(0.17f))
                Txt(Format.changePermille(-r.reductionPermille), Type.callout.copy(textAlign = TextAlign.End), Modifier.weight(0.17f))
                Box(Modifier.weight(0.16f), contentAlignment = Alignment.CenterEnd) { StarRow(stars, 11.dp) }
            }
        }
        if (hidden > 0) Txt(t("Settings with more stars than you have reached stay hidden until you find them."), Type.caption)
    }
}
