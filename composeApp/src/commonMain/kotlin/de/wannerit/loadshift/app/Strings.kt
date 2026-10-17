package de.wannerit.loadshift.app

/**
 * German texts of the app, keyed by the English text (the English text is shown as it is). Placeholders as in
 * the old string catalog: %@ for text, %lld for whole numbers, positional %1\$@ … in the translation; a
 * literal % in a text with placeholders is written %%.
 */
internal val GERMAN: Map<String, String> = mapOf(
    "%@ has no start time and must start by %@, or it misses its deadline." to
        "%1\$@ hat keine Startzeit und muss bis %2\$@ starten, sonst verpasst er seine Frist.",
    "%lld days" to
        "%lld Tage",
    "%lld days, from 00:00" to
        "%lld Tage, ab 00:00",
    "%lld jobs with deadlines" to
        "%lld Jobs mit Fristen",
    "%lld of %lld servers on" to
        "%1\$lld von %2\$lld Servern an",
    "%lld of %lld servers on · %@ kW IT power" to
        "%1\$lld von %2\$lld Servern an · %3\$@ kW IT-Leistung",
    "%lld of 3 stars" to
        "%lld von 3 Sternen",
    "%lld servers" to
        "%lld Server",
    "%lld servers for batch jobs" to
        "%lld Server für Batch-Jobs",
    "%lld servers run" to
        "%lld Server laufen",
    "%lld servers · %lld h · due %@" to
        "%1\$lld Server · %2\$lld h · fällig %3\$@",
    "%lld servers, %lld load units each" to
        "%1\$lld Server mit je %2\$lld Lasteinheiten",
    "1 day" to
        "1 Tag",
    "1 day, from 00:00" to
        "1 Tag, ab 00:00",
    "Active: %@" to
        "Aktiv: %@",
    "All on" to
        "Alle an",
    "All servers on" to
        "Alle Server an",
    "All servers on (reference)" to
        "Alle Server an (Referenz)",
    "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte die Server ab, die nicht gebraucht werden – und rechtzeitig wieder ein: Ein Server braucht 30 Minuten, bis er arbeiten kann.",
    "An idle server still draws about a quarter of its maximum power. Switch off what is not needed – but a server you switch on takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte ab, was nicht gebraucht wird – aber ein eingeschalteter Server braucht 30 Minuten, bis er arbeiten kann.",
    "Apply" to
        "Übernehmen",
    "arrives %@" to
        "kommt %@",
    "Autoscale" to
        "Autoscaling",
    "Autoscaling at %@" to
        "Autoscaling mit %@",
    "Autoscaling at 70 % (from scenario 4)" to
        "Autoscaling mit 70 % (ab Szenario 4)",
    "Autumn" to
        "Herbst",
    "Back" to
        "Zurück",
    "Back to the briefing" to
        "Zurück zum Briefing",
    "Batch job" to
        "Batch-Job",
    "Batch jobs" to
        "Batch-Jobs",
    "Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done" to
        "Batch-Jobs – hell: Ankunft bis Frist, umrandet: geplant, gefüllt: läuft oder fertig",
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
    "Best result" to
        "Bestes Ergebnis",
    "booting" to
        "startet",
    "Booting" to
        "Startet",
    "Capacity" to
        "Kapazität",
    "Capacity: %lld load units" to
        "Kapazität: %lld Lasteinheiten",
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
    "every job by its deadline and less CO₂ than the reference" to
        "jeder Job rechtzeitig und weniger CO₂ als die Referenz",
    "Every server runs all the time – like the reference operator." to
        "Alle Server laufen ständig – wie beim Referenzbetreiber.",
    "Expected load in 1 h" to
        "Erwartete Last in 1 h",
    "Fixed" to
        "Fest",
    "Fixed: %lld servers all day" to
        "Fest: %lld Server den ganzen Tag",
    "Forecast while it runs: %lld g/kWh · start by %@" to
        "Prognose während der Laufzeit: %1\$lld g/kWh · spätestens %2\$@",
    "France" to
        "Frankreich",
    "Germany" to
        "Deutschland",
    "Goals" to
        "Ziele",
    "Great Britain" to
        "Großbritannien",
    "Grid now" to
        "Netz jetzt",
    "Image conversion" to
        "Bildkonvertierung",
    "IT power" to
        "IT-Leistung",
    "Jobs done" to
        "Jobs fertig",
    "Jobs on time" to
        "Jobs rechtzeitig",
    "late, %@" to
        "zu spät, %@",
    "Load (line) and capacity of the running servers (area) – red: slow requests" to
        "Last (Linie) und Kapazität der laufenden Server (Fläche) – rot: langsame Requests",
    "Load / capacity" to
        "Last / Kapazität",
    "Mean utilization" to
        "Mittlere Auslastung",
    "mean utilization at least %@" to
        "mittlere Auslastung mindestens %@",
    "Missed" to
        "Verpasst",
    "More scenarios follow: forecasts and wind, headroom and autoscaling, cooling, hardware, AI training, eco mode, two sites." to
        "Weitere Szenarien folgen: Prognosen und Wind, Reserve und Autoscaling, Kühlung, Hardware, KI-Training, Eco-Modus, zwei Standorte.",
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
    "Plan" to
        "Planen",
    "planned" to
        "geplant",
    "Planned by the forecast (from scenario 3)" to
        "Nach Prognose geplant (ab Szenario 3)",
    "Play again" to
        "Noch einmal",
    "Poland" to
        "Polen",
    "Power" to
        "Leistung",
    "Reference" to
        "Referenz",
    "Remove the plan" to
        "Plan entfernen",
    "Right now" to
        "Gerade jetzt",
    "Right: the jobs with their size and deadline. Plan a start time for each job with plus and minus, or start it now; a planned job starts by itself. Bottom: the carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done. The game pauses when a job must start within the hour." to
        "Rechts: die Jobs mit Größe und Frist. Plane für jeden Job mit Plus und Minus eine Startzeit oder starte ihn sofort; ein geplanter Job startet von selbst. Unten: die CO₂-Intensität mit Prognose und darunter eine Zeile pro Job – hell: die Zeit zwischen Ankunft und Frist, umrandet: geplant, gefüllt: läuft oder fertig. Das Spiel hält an, wenn ein Job innerhalb der nächsten Stunde starten muss.",
    "Rule" to
        "Regel",
    "Run a greener data center" to
        "Ein Rechenzentrum klimafreundlich betreiben",
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
    "SLA at least %@ and less CO₂ than the reference" to
        "SLA mindestens %@ und weniger CO₂ als die Referenz",
    "SLA at least %@ and less CO₂ than the reference operator, who leaves every server on" to
        "SLA mindestens %@ und weniger CO₂ als der Referenzbetreiber, der alle Server laufen lässt",
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
    "Target utilization: %@" to
        "Zielauslastung: %@",
    "The game starts paused. Top: clock and speed – pause, 1×, 4×, 16× – and below it your goals. Middle: your racks; grey servers are off, light blue ones are booting, the blue bar shows how busy a server is. Right: plus and minus for the servers, and the situation right now. Bottom: carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs." to
        "Das Spiel beginnt pausiert. Oben: Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele. Mitte: deine Racks; graue Server sind aus, hellblaue starten gerade, der blaue Balken zeigt, wie ausgelastet ein Server ist. Rechts: Plus und Minus für die Server und die Lage gerade jetzt. Unten: die CO₂-Intensität des Stroms mit Prognose, die Last bisher und die erwartete Last (gepunktet); die Linien rechts zeigen, wie viele Server eine Last braucht.",
    "The idea" to
        "Die Idee",
    "The night is not clean everywhere: in a solar grid the midday hours are the cleanest. Part of the CO₂ is the hardware's manufacturing – it stays the same whenever the jobs run." to
        "Die Nacht ist nicht überall sauber: Im Solarnetz sind die Mittagsstunden am saubersten. Ein Teil des CO₂ stammt aus der Herstellung der Hardware – er bleibt gleich, egal wann die Jobs laufen.",
    "The same job emits less CO₂ when the grid is clean. In Spain's solar grid the cleanest hours are around midday, not at night. Plan every job into clean hours before its deadline – and keep in mind that only ten servers are there." to
        "Derselbe Job stößt weniger CO₂ aus, wenn der Strom sauber ist. Im spanischen Solarnetz sind die saubersten Stunden um die Mittagszeit, nicht nachts. Plane jeden Job vor seiner Frist in saubere Stunden – und denk daran, dass nur zehn Server da sind.",
    "The same run with exactly the same load and grid mix – only the rules differ:" to
        "Derselbe Ablauf mit genau derselben Last und demselben Strommix – nur die Regeln sind anders:",
    "The servers needed for the expected load of the next hour run. A low target leaves headroom for peaks but more servers idle; a high target is efficient but gets slow at peaks." to
        "Es laufen die Server, die für die erwartete Last der nächsten Stunde nötig sind. Ein niedriges Ziel lässt Reserve für Spitzen, aber mehr Server stehen im Leerlauf; ein hohes Ziel ist effizient, wird aber bei Spitzen langsam.",
    "The web shop's batch cluster in Spain: ten servers for reports, backups, imports and conversions. Over two days seven jobs arrive, each with a deadline. Nobody minds when a job runs – as long as it is finished in time." to
        "Der Batch-Cluster des Webshops in Spanien: zehn Server für Reports, Backups, Importe und Konvertierungen. In zwei Tagen kommen sieben Jobs, jeder mit einer Frist. Wann ein Job läuft, ist egal – solange er rechtzeitig fertig ist.",
    "Too late for the deadline – start by %@." to
        "Zu spät für die Frist – spätestens %@ starten.",
    "Utilization" to
        "Auslastung",
    "utilization ≥ %@" to
        "Auslastung ≥ %@",
    "Wait for the sun" to
        "Warten auf die Sonne",
    "Web requests" to
        "Web-Requests",
    "What if …" to
        "Was wäre, wenn …",
    "Whole year" to
        "Jahresmittel",
    "You" to
        "Du",
    "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on." to
        "Du betreibst das Rechenzentrum eines Webshops in Deutschland. Seine 20 Server sind für das Weihnachtsgeschäft ausgelegt. Heute ist ein gewöhnlicher Herbsttag: Abends kaufen viele ein, nachts kaum jemand. Es ist Mitternacht, und die vorige Schicht hat alle Server laufen lassen.",
)
