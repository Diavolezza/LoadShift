package de.wannerit.loadshift.app

/**
 * German texts of the app, keyed by the English text (the English text is shown as it is). Placeholders as in
 * the old string catalog: %@ for text, %lld for whole numbers, positional %1\$@ … in the translation; a
 * literal % in a text with placeholders is written %%.
 */
internal val GERMAN: Map<String, String> = mapOf(
    "%@ has no start time and must start by %@, or it misses its deadline." to
        "%1\$@ hat keine Startzeit und muss bis %2\$@ starten, sonst verpasst er seine Frist.",
    "%@, with deadline safety" to
        "%@, mit Fristsicherung",
    "%@, without deadline safety" to
        "%@, ohne Fristsicherung",
    "%lld days" to
        "%lld Tage",
    "%lld days, from 00:00" to
        "%lld Tage, ab 00:00",
    "%lld jobs with deadlines" to
        "%lld Jobs mit Fristen",
    "%lld of %lld servers on · %@ kW IT power" to
        "%1\$lld von %2\$lld Servern an · %3\$@ kW IT-Leistung",
    "%lld of %lld servers on" to
        "%1\$lld von %2\$lld Servern an",
    "%lld of 3 stars" to
        "%lld von 3 Sternen",
    "%lld servers for batch jobs" to
        "%lld Server für Batch-Jobs",
    "%lld servers run all the time (%lld load units)." to
        "%1\$lld Server laufen die ganze Zeit (%2\$lld Lasteinheiten).",
    "%lld servers run" to
        "%lld Server laufen",
    "%lld servers · %lld h · due %@" to
        "%1\$lld Server · %2\$lld h · fällig %3\$@",
    "%lld servers" to
        "%lld Server",
    "%lld servers, %lld load units each" to
        "%1\$lld Server mit je %2\$lld Lasteinheiten",
    "1 day" to
        "1 Tag",
    "1 day, from 00:00" to
        "1 Tag, ab 00:00",
    "A fixed threshold works while the wind blows. When it drops, the jobs wait in vain: without the deadline safety they miss their deadlines, with it they start at the last moment – often in the dirtiest hours. A rule that compares the hours of the forecast adapts to the weather – as long as the plan is made again when the forecast changes. Real schedulers plan again regularly for this reason." to
        "Ein fester Schwellwert funktioniert, solange der Wind weht. Lässt er nach, warten die Jobs vergeblich: Ohne Fristsicherung verpassen sie ihre Fristen, mit ihr starten sie im letzten Moment – oft in den schmutzigsten Stunden. Eine Regel, die die Stunden der Prognose vergleicht, passt sich dem Wetter an – solange neu geplant wird, wenn sich die Prognose ändert. Echte Scheduler planen deshalb regelmäßig neu.",
    "A job starts as soon as the grid is at or below %lld g/kWh – the value now, not the forecast." to
        "Ein Job startet, sobald der Strom höchstens %lld g/kWh hat – der Wert jetzt, nicht die Prognose.",
    "A job that would otherwise miss its deadline starts when there is just enough time left (with a fixed hour and a threshold)." to
        "Ein Job, der sonst seine Frist verpassen würde, startet, wenn die Zeit gerade noch reicht (bei fester Uhrzeit und Schwellwert).",
    "A rule decides when each job starts – you no longer plan every job by hand. Choose a rule and apply it; you can change it at any time." to
        "Eine Regel entscheidet, wann jeder Job startet – du planst nicht mehr jeden Job von Hand. Wähle eine Regel und übernimm sie; du kannst sie jederzeit ändern.",
    "A rule switches the servers – you no longer do it by hand. One server handles %lld load units; it takes 30 minutes to boot." to
        "Eine Regel schaltet die Server – du machst es nicht mehr von Hand. Ein Server schafft %lld Lasteinheiten und braucht 30 Minuten zum Starten.",
    "A scheduler starts jobs by rules that look at the carbon intensity – now or in the forecast. It automates Time Shifting; Google shifts flexible work in its data centers this way, with day-ahead forecasts." to
        "Ein Scheduler startet Jobs nach Regeln, die auf die CO₂-Intensität schauen – jetzt oder in der Prognose. Das automatisiert Time Shifting; Google verschiebt so flexible Arbeit in seinen Rechenzentren, mit Prognosen für den nächsten Tag.",
    "A teaching game about Green IT: run a data center so that it serves its users reliably and emits as little CO₂ as possible – by switching servers, shifting work in time and, later, writing better software." to
        "Ein Lernspiel zu Green IT: Betreibe ein Rechenzentrum so, dass es seine Nutzer zuverlässig bedient und möglichst wenig CO₂ ausstößt – durch Schalten von Servern, Verschieben von Arbeit in der Zeit und später durch bessere Software.",
    "About Load Shift" to
        "Über Load Shift",
    "Active: %@" to
        "Aktiv: %@",
    "All %lld servers run all the time." to
        "Alle %lld Server laufen die ganze Zeit.",
    "All on" to
        "Alle an",
    "all on" to
        "alle an",
    "All servers on (reference)" to
        "Alle Server an (Referenz)",
    "All servers on" to
        "Alle Server an",
    "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte die Server ab, die nicht gebraucht werden – und rechtzeitig wieder ein: Ein Server braucht 30 Minuten, bis er arbeiten kann.",
    "An idle server still draws about a quarter of its maximum power. Switch off what is not needed – but a server you switch on takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte ab, was nicht gebraucht wird – aber ein eingeschalteter Server braucht 30 Minuten, bis er arbeiten kann.",
    "Apply" to
        "Übernehmen",
    "arrives %@" to
        "kommt %@",
    "As soon as possible" to
        "So früh wie möglich",
    "as soon as possible" to
        "so früh wie möglich",
    "at %@" to
        "um %@ Uhr",
    "At a fixed hour" to
        "Zu einer festen Uhrzeit",
    "At least %lld servers every day from %@ to %@ (scheduled scaling)." to
        "Mindestens %1\$lld Server, jeden Tag von %2\$@ bis %3\$@ (Scheduled Scaling).",
    "Autoscale" to
        "Autoscaling",
    "Autoscaling at %@ (as at the start)" to
        "Autoscaling bei %@ (wie zu Beginn)",
    "Autoscaling at %@" to
        "Autoscaling bei %@",
    "autoscaling at %@" to
        "Autoscaling bei %@",
    "autoscaling at %@, at least %lld servers %@–%@" to
        "Autoscaling bei %1\$@, mindestens %2\$lld Server %3\$@–%4\$@",
    "Autoscaling at %@, every day at least %lld servers 18:00–22:00" to
        "Autoscaling bei %1\$@, jeden Tag mindestens %2\$lld Server 18:00–22:00",
    "Autoscaling at %@, on day 2 at least %lld servers 18:00–22:00" to
        "Autoscaling bei %1\$@, an Tag 2 mindestens %2\$lld Server 18:00–22:00",
    "Autoscaling at 70 % (from scenario 4)" to
        "Autoscaling mit 70 % (ab Szenario 4)",
    "Autoscaling does what you did by hand in scenario 1: it switches servers on and off for the load. The target utilization decides how much headroom stays free. Little headroom saves energy – but a server takes 30 minutes to boot, and the autoscaler knows only the usual day; a sudden peak finds it unprepared. Headroom all the time costs energy all the time; a minimum for a known event costs it only when it is needed." to
        "Autoscaling macht, was du in Szenario 1 von Hand gemacht hast: Es schaltet Server passend zur Last ein und aus. Die Zielauslastung bestimmt, wie viel Reserve frei bleibt. Wenig Reserve spart Energie – aber ein Server braucht 30 Minuten zum Starten, und das Autoscaling kennt nur den gewöhnlichen Tag; eine plötzliche Spitze trifft es unvorbereitet. Reserve rund um die Uhr kostet rund um die Uhr Energie; ein Minimum für ein bekanntes Ereignis kostet sie nur, wenn sie gebraucht wird.",
    "Autumn" to
        "Herbst",
    "Back to the briefing" to
        "Zurück zum Briefing",
    "Back" to
        "Zurück",
    "Batch job" to
        "Batch-Job",
    "Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done" to
        "Batch-Jobs – hell: Ankunft bis Frist, umrandet: geplant, gefüllt: läuft oder fertig",
    "Batch jobs" to
        "Batch-Jobs",
    "Batch rule" to
        "Batch-Regel",
    "batch work at %lld g/kWh or less on average" to
        "Batch-Arbeit im Schnitt bei höchstens %lld g/kWh",
    "Batch work Ø %lld g/kWh (reference %lld)" to
        "Batch-Arbeit Ø %1\$lld g/kWh (Referenz %2\$lld)",
    "Batch work, mean g/kWh" to
        "Batch-Arbeit, Ø g/kWh",
    "Batch work: no job has run yet" to
        "Batch-Arbeit: noch kein Job gelaufen",
    "batch ≤ %lld g/kWh" to
        "Batch ≤ %lld g/kWh",
    "below %lld g/kWh" to
        "unter %lld g/kWh",
    "Below 150 g/kWh, with deadline safety" to
        "Unter 150 g/kWh, mit Fristsicherung",
    "Below 150 g/kWh, without deadline safety" to
        "Unter 150 g/kWh, ohne Fristsicherung",
    "Below a threshold" to
        "Unter einem Schwellwert",
    "Best result" to
        "Bestes Ergebnis",
    "Booting" to
        "Startet",
    "booting" to
        "startet",
    "Built with Kotlin and Compose Multiplatform" to
        "Gebaut mit Kotlin und Compose Multiplatform",
    "by hand" to
        "von Hand",
    "Campaign" to
        "Kampagne",
    "Capacity follows the load automatically: servers are added when the utilization rises above a target and removed when it falls. The target decides the headroom. Scheduled scaling adds capacity for known events in advance." to
        "Die Kapazität folgt der Last automatisch: Server kommen hinzu, wenn die Auslastung über ein Ziel steigt, und gehen, wenn sie fällt. Das Ziel bestimmt die Reserve (Headroom). Scheduled Scaling stellt für bekannte Ereignisse vorab Kapazität bereit.",
    "Capacity rule" to
        "Kapazitätsregel",
    "Capacity" to
        "Kapazität",
    "Capacity: %lld load units" to
        "Kapazität: %lld Lasteinheiten",
    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted). The campaign is marked – but it is not in the expected load." to
        "Die CO₂-Intensität des Stroms mit Prognose, die Last bisher und die erwartete Last (gepunktet). Die Kampagne ist markiert – aber nicht in der erwarteten Last.",
    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs." to
        "Die CO₂-Intensität des Stroms mit Prognose, die Last bisher und die erwartete Last (gepunktet); die Linien rechts zeigen, wie viele Server eine Last braucht.",
    "Carbon intensity of the grid, g CO₂/kWh – dashed: forecast" to
        "CO₂-Intensität des Stroms, g CO₂/kWh – gestrichelt: Prognose",
    "compared with the reference operator, who leaves every server on." to
        "im Vergleich zum Referenzbetreiber, der alle Server laufen lässt.",
    "compared with the reference operator, who starts every job as soon as it arrives." to
        "im Vergleich zum Referenzbetreiber, der jeden Job sofort bei Ankunft startet.",
    "CO₂ %@ or better" to
        "CO₂ %@ oder besser",
    "CO₂ from %@" to
        "CO₂ ab %@",
    "CO₂ so far" to
        "CO₂ bisher",
    "CO₂ vs. reference" to
        "CO₂ ggü. Referenz",
    "CO₂ vs. reference: %@" to
        "CO₂ ggü. Referenz: %@",
    "CO₂ vs. you" to
        "CO₂ ggü. dir",
    "Daily report" to
        "Tagesreport",
    "Data import" to
        "Datenimport",
    "Day %lld" to
        "Tag %lld",
    "Day %lld, %@" to
        "Tag %1\$lld, %2\$@",
    "Day complete" to
        "Tag geschafft",
    "Deadline safety" to
        "Fristsicherung",
    "Deadlines missed: %lld" to
        "Fristen verpasst: %lld",
    "done %@" to
        "fertig %@",
    "Each job needs servers for some hours and must be finished by its deadline. When it starts, its servers switch on and boot for 30 minutes first; servers without a job are off." to
        "Jeder Job braucht einige Stunden lang Server und muss bis zu seiner Frist fertig sein. Beim Start schalten sich seine Server ein und starten zuerst 30 Minuten lang; Server ohne Job sind aus.",
    "Earn a star in the scenario before." to
        "Hol dir einen Stern im Szenario davor.",
    "Energy (kWh)" to
        "Energie (kWh)",
    "Evening peak about %lld load units" to
        "Abendspitze etwa %lld Lasteinheiten",
    "Every job as soon as it arrives (reference)" to
        "Jeder Job sofort bei Ankunft (Referenz)",
    "Every job at 11:00" to
        "Jeder Job um 11:00",
    "Every job at night, at 01:00" to
        "Jeder Job nachts um 01:00",
    "Every job at night, at 03:00" to
        "Jeder Job nachts um 03:00",
    "every job by its deadline and less CO₂ than the reference" to
        "jeder Job rechtzeitig und weniger CO₂ als die Referenz",
    "Every job is planned into the hours with the lowest forecast before its deadline, once, when you apply the rule." to
        "Jeder Job wird in die Stunden mit der niedrigsten Prognose vor seiner Frist geplant – einmal, wenn du die Regel übernimmst.",
    "Every job starts at the next %@ after it arrives." to
        "Jeder Job startet nach seiner Ankunft beim nächsten Mal um %@ Uhr.",
    "Every job starts when it arrives." to
        "Jeder Job startet, sobald er da ist.",
    "Every server runs all the time – like the reference operator." to
        "Alle Server laufen ständig – wie beim Referenzbetreiber.",
    "Expected load in 1 h" to
        "Erwartete Last in 1 h",
    "fixed %lld servers" to
        "fest %lld Server",
    "Fixed number" to
        "Feste Anzahl",
    "Fixed" to
        "Fest",
    "Fixed: %lld servers all day" to
        "Fest: %lld Server den ganzen Tag",
    "Font: Inter, SIL Open Font License 1.1" to
        "Schrift: Inter, SIL Open Font License 1.1",
    "Forecast update: the wind drops about 12 hours earlier than forecast." to
        "Neue Prognose: Der Wind lässt etwa 12 Stunden früher nach als vorhergesagt.",
    "Forecast while it runs: %lld g/kWh · start by %@" to
        "Prognose während der Laufzeit: %1\$lld g/kWh · spätestens %2\$@",
    "France" to
        "Frankreich",
    "from %@" to
        "ab %@",
    "Germany" to
        "Deutschland",
    "Goals" to
        "Ziele",
    "Great Britain" to
        "Großbritannien",
    "Green IT – from code to data center" to
        "Green IT – vom Code bis zum Rechenzentrum",
    "Greenest hours (forecast)" to
        "Sauberste Stunden (Prognose)",
    "greenest hours" to
        "sauberste Stunden",
    "Greenest hours, planned again after the forecast update" to
        "Sauberste Stunden, nach der neuen Prognose neu geplant",
    "Greenest hours, planned once" to
        "Sauberste Stunden, einmal geplant",
    "Grid data: Electricity Maps, Grid Review 2025 (life-cycle, imports included)" to
        "Netzdaten: Electricity Maps, Grid Review 2025 (Lebenszyklus, Importe eingerechnet)",
    "Grid now" to
        "Netz jetzt",
    "Headroom is insurance: it costs energy every hour, whether the peak comes or not. When a peak is known in advance, scheduled scaling is cheaper – cloud platforms offer it next to the autoscaler. Predictive autoscaling goes one step further and learns such peaks from the past." to
        "Reserve ist eine Versicherung: Sie kostet jede Stunde Energie, ob die Spitze kommt oder nicht. Ist eine Spitze vorher bekannt, ist Scheduled Scaling günstiger – Cloud-Plattformen bieten das neben dem Autoscaling an. Predictive Autoscaling geht einen Schritt weiter und lernt solche Spitzen aus der Vergangenheit.",
    "Image conversion" to
        "Bildkonvertierung",
    "IT power" to
        "IT-Leistung",
    "Jobs done" to
        "Jobs fertig",
    "Jobs on time" to
        "Jobs rechtzeitig",
    "Keeps enough servers on for the expected load of the next hour – or the measured load, if it is higher – at %@ utilization. The rest is headroom." to
        "Hält genug Server für die erwartete Last der nächsten Stunde – oder die gemessene, falls sie höher ist – bei %@ Auslastung. Der Rest ist Reserve.",
    "late, %@" to
        "zu spät, %@",
    "Load (line) and capacity of the running servers (area) – red: slow requests" to
        "Last (Linie) und Kapazität der laufenden Server (Fläche) – rot: langsame Requests",
    "Load / capacity" to
        "Last / Kapazität",
    "mean utilization at least %@" to
        "mittlere Auslastung mindestens %@",
    "Mean utilization" to
        "Mittlere Auslastung",
    "Minimum in a time window" to
        "Minimum in einem Zeitfenster",
    "Missed" to
        "Verpasst",
    "MIT License" to
        "MIT-Lizenz",
    "More scenarios follow: headroom and autoscaling, consolidation, cooling, hardware, AI training, eco mode, two sites – and an engineering track: profiling and growing data." to
        "Weitere Szenarien folgen: Reserve und Autoscaling, Konsolidierung, Kühlung, Hardware, KI-Training, Eco-Modus, zwei Standorte – und ein Engineering-Strang: Profiling und wachsende Daten.",
    "Moving flexible work into the hours when the grid is cleanest. Together with Location Shifting it is called Demand Shifting – part of the Green Software Foundation principle Carbon Awareness." to
        "Flexible Arbeit in die Stunden verschieben, in denen der Strom am saubersten ist. Zusammen mit Location Shifting heißt das Demand Shifting – ein Teil des Prinzips Carbon Awareness der Green Software Foundation.",
    "New rule" to
        "Neue Regel",
    "Night shift" to
        "Nachtschicht",
    "No deadline missed" to
        "Keine Frist verpasst",
    "Norway" to
        "Norwegen",
    "not planned" to
        "nicht geplant",
    "of %lld servers on" to
        "von %lld Servern an",
    "of it embodied (kg)" to
        "davon Herstellung (kg)",
    "Off" to
        "Aus",
    "On the screen" to
        "Auf dem Bildschirm",
    "One server handles %lld load units (%lld,000 requests per 15 minutes). Above 85 %% utilization requests get slow." to
        "Ein Server schafft %1\$lld Lasteinheiten (%2\$lld.000 Requests pro 15 Minuten). Über 85 %% Auslastung werden Requests langsam.",
    "One star opens the next scenario." to
        "Ein Stern schaltet das nächste Szenario frei.",
    "Plan again with the latest forecast" to
        "Mit neuester Prognose neu planen",
    "Plan" to
        "Planen",
    "planned %@" to
        "geplant %@",
    "Planned by the forecast (from scenario 3)" to
        "Nach Prognose geplant (ab Szenario 3)",
    "Planned with the forecast of %@." to
        "Geplant mit der Prognose von %@.",
    "planned" to
        "geplant",
    "Planning every job by hand does not scale. A rule does it for you: at a fixed hour, below a threshold, or into the greenest hours of the forecast. But the forecast is uncertain – ±25 % a day ahead in a wind grid – and a rule that waits for clean power may wait in vain. The deadline safety makes sure that no job misses its deadline." to
        "Jeden Job von Hand zu planen, skaliert nicht. Eine Regel übernimmt das: zu einer festen Uhrzeit, unter einem Schwellwert oder in die saubersten Stunden der Prognose. Doch die Prognose ist unsicher – in einem Windnetz ±25 % einen Tag im Voraus –, und eine Regel, die auf sauberen Strom wartet, wartet vielleicht vergeblich. Die Fristsicherung sorgt dafür, dass kein Job seine Frist verpasst.",
    "Plans made with the old forecast may now fall into dirty hours. Look at the new forecast below." to
        "Pläne, die mit der alten Prognose gemacht wurden, liegen jetzt vielleicht in schmutzigen Stunden. Schau dir unten die neue Prognose an.",
    "Play again" to
        "Noch einmal",
    "Plus and minus for the servers, and the situation right now." to
        "Plus und Minus für die Server und die Lage gerade jetzt.",
    "Poland" to
        "Polen",
    "Power" to
        "Leistung",
    "Recommendations" to
        "Empfehlungen",
    "Reference" to
        "Referenz",
    "Remove the plan" to
        "Plan entfernen",
    "Right now" to
        "Gerade jetzt",
    "Rule" to
        "Regel",
    "Running only as much capacity as the load needs. An idle server still draws power, so servers that are not needed are switched off. Green Software Foundation principle: energy efficiency." to
        "Nur so viel Kapazität betreiben, wie die Last braucht. Ein Server im Leerlauf zieht trotzdem Strom, deshalb werden nicht benötigte Server abgeschaltet. Prinzip der Green Software Foundation: Energy Efficiency.",
    "runs until %@" to
        "läuft bis %@",
    "Scenario %lld: %@" to
        "Szenario %1\$lld: %2\$@",
    "Scenario complete" to
        "Szenario geschafft",
    "Search index" to
        "Suchindex",
    "Servers in use: %lld of %lld" to
        "Server in Benutzung: %1\$lld von %2\$lld",
    "Servers on (mean)" to
        "Server an (Mittel)",
    "Servers working" to
        "Server in Betrieb",
    "Situation" to
        "Ausgangslage",
    "SLA at least %@ and less CO₂ than the reference operator, who leaves every server on" to
        "SLA mindestens %@ und weniger CO₂ als der Referenzbetreiber, der alle Server laufen lässt",
    "SLA at least %@ and less CO₂ than the reference" to
        "SLA mindestens %@ und weniger CO₂ als die Referenz",
    "SLA at least 99 % and less CO₂ than the reference" to
        "SLA mindestens 99 % und weniger CO₂ als die Referenz",
    "Spain" to
        "Spanien",
    "Spring" to
        "Frühling",
    "start by %@" to
        "spätestens %@",
    "Start now" to
        "Jetzt starten",
    "Summer" to
        "Sommer",
    "Switching servers off saves energy, not hardware: the embodied emissions stay the same." to
        "Abschalten spart Strom, aber keine Hardware: Die Herstellungsemissionen bleiben gleich.",
    "Takes effect at once. Switched-off servers stop immediately; switched-on servers boot for 30 minutes." to
        "Wirkt sofort. Abgeschaltete Server stoppen sofort, eingeschaltete starten 30 Minuten lang.",
    "Target utilization" to
        "Zielauslastung",
    "Target utilization: %@" to
        "Zielauslastung: %@",
    "Technical term" to
        "Fachbegriff",
    "The batch rule: choose one, set it and apply it. Below it the jobs with their deadlines and what the rule has planned for them." to
        "Die Batch-Regel: wählen, einstellen, übernehmen. Darunter die Jobs mit ihren Fristen und was die Regel für sie geplant hat.",
    "The capacity rule: all on, a fixed number, or autoscaling with a target utilization and, if you want, a minimum for a time window. Choose, set, apply. Below it the situation right now." to
        "Die Kapazitätsregel: alle an, eine feste Anzahl oder Autoscaling mit Zielauslastung und auf Wunsch einem Minimum für ein Zeitfenster. Wählen, einstellen, übernehmen. Darunter die Lage gerade jetzt.",
    "The carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done." to
        "Die CO₂-Intensität mit Prognose und darunter eine Zeile pro Job – hell: die Zeit zwischen Ankunft und Frist, umrandet: geplant, gefüllt: läuft oder fertig.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. The game also pauses when a job must start within the hour." to
        "Das Spiel beginnt pausiert. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele. Das Spiel hält auch an, wenn ein Job innerhalb der nächsten Stunde starten muss.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals." to
        "Das Spiel beginnt pausiert. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele.",
    "The idea" to
        "Die Idee",
    "The jobs with their size and deadline. Plan a start time for each job with plus and minus, or start it now; a planned job starts by itself." to
        "Die Jobs mit Größe und Frist. Plane für jeden Job mit Plus und Minus eine Startzeit oder starte ihn sofort; ein geplanter Job startet von selbst.",
    "The night is not clean everywhere: in a solar grid the midday hours are the cleanest. Part of the CO₂ is the hardware's manufacturing – it stays the same whenever the jobs run." to
        "Die Nacht ist nicht überall sauber: Im Solarnetz sind die Mittagsstunden am saubersten. Ein Teil des CO₂ stammt aus der Herstellung der Hardware – er bleibt gleich, egal wann die Jobs laufen.",
    "The same job emits less CO₂ when the grid is clean. In Spain's solar grid the cleanest hours are around midday, not at night. Plan every job into clean hours before its deadline – and keep in mind that only ten servers are there." to
        "Derselbe Job stößt weniger CO₂ aus, wenn der Strom sauber ist. Im spanischen Solarnetz sind die saubersten Stunden um die Mittagszeit, nicht nachts. Plane jeden Job vor seiner Frist in saubere Stunden – und denk daran, dass nur zehn Server da sind.",
    "The same run with exactly the same load and grid mix – only the rules differ:" to
        "Derselbe Ablauf mit genau derselben Last und demselben Strommix – nur die Regeln sind anders:",
    "The servers needed for the expected load of the next hour run. A low target leaves headroom for peaks but more servers idle; a high target is efficient but gets slow at peaks." to
        "Es laufen die Server, die für die erwartete Last der nächsten Stunde nötig sind. Ein niedriges Ziel lässt Reserve für Spitzen, aber mehr Server stehen im Leerlauf; ein hohes Ziel ist effizient, wird aber bei Spitzen langsam.",
    "The web shop again, now in winter and with 30 servers. Autoscaling is on: at 80 % target utilization it keeps just enough servers running. On day 2 marketing sends a newsletter at 19:00 – for three hours twice as many people will shop as usual." to
        "Wieder der Webshop, jetzt im Winter und mit 30 Servern. Autoscaling ist eingeschaltet: Mit 80 % Zielauslastung hält es gerade genug Server am Laufen. An Tag 2 verschickt das Marketing um 19:00 einen Newsletter – drei Stunden lang kaufen doppelt so viele Leute ein wie sonst.",
    "The web shop's analytics cluster in Great Britain: twelve servers, nine jobs in three days. It is winter, and the British grid depends on the wind. Today is an ordinary winter day; tomorrow a storm front brings plenty of wind power, the day after the wind drops and gas power plants take over." to
        "Der Analyse-Cluster des Webshops in Großbritannien: zwölf Server, neun Jobs in drei Tagen. Es ist Winter, und das britische Netz hängt am Wind. Heute ist ein gewöhnlicher Wintertag; morgen bringt eine Sturmfront viel Windstrom, übermorgen lässt der Wind nach, und Gaskraftwerke übernehmen.",
    "The web shop's batch cluster in Spain: ten servers for reports, backups, imports and conversions. Over two days seven jobs arrive, each with a deadline. Nobody minds when a job runs – as long as it is finished in time." to
        "Der Batch-Cluster des Webshops in Spanien: zehn Server für Reports, Backups, Importe und Konvertierungen. In zwei Tagen kommen sieben Jobs, jeder mit einer Frist. Wann ein Job läuft, ist egal – solange er rechtzeitig fertig ist.",
    "This rule is active." to
        "Diese Regel ist aktiv.",
    "to %@" to
        "bis %@",
    "To scenario %lld" to
        "Weiter zu Szenario %lld",
    "Too late for the deadline – start by %@." to
        "Zu spät für die Frist – spätestens %@ starten.",
    "utilization ≥ %@" to
        "Auslastung ≥ %@",
    "Utilization" to
        "Auslastung",
    "Version %@" to
        "Version %@",
    "Wait for the sun" to
        "Warten auf die Sonne",
    "waiting" to
        "wartet",
    "Web requests" to
        "Web-Requests",
    "What if …" to
        "Was wäre, wenn …",
    "What you did is called %@. %@" to
        "Was du gemacht hast, heißt %1\$@. %2\$@",
    "When the wind drops" to
        "Wenn der Wind nachlässt",
    "Whole year" to
        "Jahresmittel",
    "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on." to
        "Du betreibst das Rechenzentrum eines Webshops in Deutschland. Seine 20 Server sind für das Weihnachtsgeschäft ausgelegt. Heute ist ein gewöhnlicher Herbsttag: Abends kaufen viele ein, nachts kaum jemand. Es ist Mitternacht, und die vorige Schicht hat alle Server laufen lassen.",
    "You" to
        "Du",
    "Your batch cluster: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job." to
        "Dein Batch-Cluster: graue Server sind aus, hellblaue starten gerade, der orange Balken zeigt einen Server, der an einem Job arbeitet.",
    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is." to
        "Deine Racks: graue Server sind aus, hellblaue starten gerade, der blaue Balken zeigt, wie ausgelastet ein Server ist.",
)
