package de.wannerit.loadshift.app

/**
 * German texts of the app, keyed by the English text (the English text is shown as it is). Placeholders as in
 * the old string catalog: %@ for text, %lld for whole numbers, positional %1\$@ … in the translation; a
 * literal % in a text with placeholders is written %%.
 */
internal val GERMAN: Map<String, String> = mapOf(
    "%@ and %@" to
        "%1\$@ und %2\$@",
    "%@ has no start time and must start by %@, or it misses its deadline." to
        "%1\$@ hat keine Startzeit und muss bis %2\$@ starten, sonst verpasst er seine Frist.",
    "%@ needs %lld GB – host %lld has only %lld GB free." to
        "%1\$@ braucht %2\$lld GB – Host %3\$lld hat nur %4\$lld GB frei.",
    "%@ of the viewers" to
        "%@ der Zuschauer",
    "%@ · grid on average %lld g/kWh" to
        "%1\$@ · Strommix im Mittel %2\$lld g/kWh",
    "%@, eco %@ above %lld g/kWh" to
        "%1\$@, Eco %2\$@ über %3\$lld g/kWh",
    "%@, setpoint %lld °C" to
        "%1\$@, Sollwert %2\$lld °C",
    "%@, with deadline safety" to
        "%@, mit Fristsicherung",
    "%@, without deadline safety" to
        "%@, ohne Fristsicherung",
    "%@: %@ GB" to
        "%1\$@: %2\$@ GB",
    "%@: %@ t CO₂ (%@ t hardware), capacity %lld, peak %lld" to
        "%1\$@: %2\$@ t CO₂ (%3\$@ t Hardware), Kapazität %4\$lld, Spitze %5\$lld",
    "%@: %@ → %@" to
        "%1\$@: %2\$@ → %3\$@",
    "%@: peak %lld" to
        "%1\$@: Spitze %2\$lld",
    "%lld cache servers" to
        "%lld Cache-Server",
    "%lld cache servers, %lld kinds cached" to
        "%1\$lld Cache-Server, %2\$lld Arten im Cache",
    "%lld days" to
        "%lld Tage",
    "%lld days, from 00:00" to
        "%lld Tage, ab 00:00",
    "%lld GB free" to
        "%lld GB frei",
    "%lld GB memory, %lld W all day" to
        "%1\$lld GB Speicher, %2\$lld W den ganzen Tag",
    "%lld GB RAM · peak %lld load units" to
        "%1\$lld GB RAM · Spitze %2\$lld Lasteinheiten",
    "%lld GPU servers on, progress %@" to
        "%1\$lld GPU-Server an, Fortschritt %2\$@",
    "%lld hosts in use" to
        "%lld Hosts in Benutzung",
    "%lld jobs with deadlines" to
        "%lld Jobs mit Fristen",
    "%lld of %lld" to
        "%1\$lld von %2\$lld",
    "%lld of %lld GB" to
        "%1\$lld von %2\$lld GB",
    "%lld of %lld servers on" to
        "%1\$lld von %2\$lld Servern an",
    "%lld of %lld servers on + %lld cache servers · %@ kW IT power" to
        "%1\$lld von %2\$lld Servern an + %3\$lld Cache-Server · %4\$@ kW IT-Leistung",
    "%lld of %lld servers on · %@ kW IT power" to
        "%1\$lld von %2\$lld Servern an · %3\$@ kW IT-Leistung",
    "%lld of %lld story points" to
        "%1\$lld von %2\$lld Story Points",
    "%lld of %lld story points – %lld carry over" to
        "%1\$lld von %2\$lld Story Points – %3\$lld laufen weiter",
    "%lld of 3 stars" to
        "%lld von 3 Sternen",
    "%lld old servers (%@ years), %lld failed; %lld new" to
        "%1\$lld alte Server (%2\$@ Jahre), %3\$lld ausgefallen; %4\$lld neue",
    "%lld servers" to
        "%lld Server",
    "%lld servers for batch jobs" to
        "%lld Server für Batch-Jobs",
    "%lld servers in %@, %lld in %@" to
        "%1\$lld Server in %2\$@, %3\$lld in %4\$@",
    "%lld servers run" to
        "%lld Server laufen",
    "%lld servers run all the time (%lld load units)." to
        "%1\$lld Server laufen die ganze Zeit (%2\$lld Lasteinheiten).",
    "%lld servers · %lld h · %@ – %@" to
        "%1\$lld Server · %2\$lld h · %3\$@ – %4\$@",
    "%lld servers · %lld h · due %@" to
        "%1\$lld Server · %2\$lld h · fällig %3\$@",
    "%lld servers, %lld load units each" to
        "%1\$lld Server mit je %2\$lld Lasteinheiten",
    "%lld SP" to
        "%lld SP",
    "1 day" to
        "1 Tag",
    "1 day, from 00:00" to
        "1 Tag, ab 00:00",
    "20 currencies, fetched from the bank." to
        "20 Währungen, von der Bank geholt.",
    "20,000 images of about 2 MB each." to
        "20.000 Bilder mit je etwa 2 MB.",
    "4 servers, cap 70 %, at once" to
        "4 Server, Cap 70 %, sofort",
    "4K on every device (reference)" to
        "4K auf jedem Gerät (Referenz)",
    "50,000 pages of about 500 KB, expensive to build; new prices on day 2 at 18:00." to
        "50.000 Seiten mit je etwa 500 KB, aufwendig zu bauen; neue Preise an Tag 2 um 18:00.",
    "6 servers, cap 60 %, greenest hours" to
        "6 Server, Cap 60 %, grünste Stunden",
    "8 servers, cap 70 %, at once" to
        "8 Server, Cap 70 %, sofort",
    "8 servers, cap 70 %, greenest hours" to
        "8 Server, Cap 70 %, grünste Stunden",
    "8 servers, no cap, at once (reference)" to
        "8 Server, kein Cap, sofort (Referenz)",
    "8 servers, no cap, greenest hours" to
        "8 Server, kein Cap, grünste Stunden",
    "A cache is a bet on repetition. It wins where many requests ask for the same thing – images, rates, popular pages – and loses where the answer must be current or is asked only once: a cached account balance is an error, a cached search that nobody repeats only fills memory. Size the memory by what it holds, not by what might come: the hot set fits into one server, more servers only draw power. And a cache must learn when its data change: the TTL bounds the age in normal times, but after a price change only invalidation keeps the answers right – the hard part of caching is not keeping, but forgetting at the right moment. Warming the cache every night sounds diligent, but the pages expire after 15 minutes – long before the first customer comes: work for nothing. HyperLogLog and the materialized view follow the same idea: keep a little, compute less – as long as the business accepts the result." to
        "Ein Cache ist eine Wette auf Wiederholung. Er gewinnt, wo viele Anfragen dasselbe wollen – Bilder, Kurse, beliebte Seiten –, und verliert, wo die Antwort aktuell sein muss oder nur einmal gefragt wird: Ein gecachter Kontostand ist ein Fehler, eine gecachte Suche, die niemand wiederholt, füllt nur den Speicher. Bemiss den Speicher nach dem, was er hält, nicht nach dem, was kommen könnte: Der heiße Bestand passt auf einen Server, weitere Server ziehen nur Strom. Und ein Cache muss erfahren, wann sich seine Daten ändern: Die TTL begrenzt das Alter im Normalfall, aber nach einer Preisänderung hält nur die Invalidierung die Antworten richtig – das Schwere am Caching ist nicht das Aufbewahren, sondern das Vergessen im richtigen Moment. Den Cache jede Nacht vorzuwärmen klingt fleißig, aber die Seiten laufen nach 15 Minuten ab – lange bevor der erste Kunde kommt: Arbeit für nichts. HyperLogLog und die materialisierte Sicht folgen derselben Idee: wenig aufbewahren, weniger rechnen – solange die Fachlichkeit das Ergebnis akzeptiert.",
    "A cache keeps answers for a while, so that the next request is cheap. It works because a few items get most of the requests – the bestsellers, the euro rate. Four things decide: what may be cached at all, how long an entry may live (the TTL – a business question, not a technical one), whether the same thing is asked again, and how much memory is worth its power." to
        "Ein Cache hält Antworten eine Zeit lang vor, damit die nächste Anfrage billig wird. Das funktioniert, weil wenige Dinge die meisten Anfragen bekommen – die Bestseller, der Eurokurs. Vier Dinge entscheiden: was überhaupt in den Cache darf, wie lange ein Eintrag lebt (die TTL – eine fachliche Frage, keine technische), ob dasselbe noch einmal gefragt wird und wie viel Speicher seinen Strom wert ist.",
    "A cache keeps answers so they need not be computed or fetched again. All kinds you tick share the memory of the cache servers; when it is full, the entry used least recently goes first (LRU). A cache server draws power all day. How old an answer may be is a business decision." to
        "Ein Cache hält Antworten vor, damit sie nicht neu berechnet oder geholt werden müssen. Alle Arten, die du ankreuzt, teilen sich den Speicher der Cache-Server; ist er voll, fliegt der am längsten nicht genutzte Eintrag zuerst (LRU). Ein Cache-Server zieht den ganzen Tag Strom. Wie alt eine Antwort sein darf, entscheidet die Fachlichkeit.",
    "A cap does not make the GPUs work less: they stay fully busy, only at a lower clock – and the last watts bring little speed. Below a certain cap, though, the rest of the server runs so much longer that it costs more than it saves." to
        "Ein Cap lässt die GPUs nicht weniger arbeiten: Sie bleiben voll ausgelastet, nur mit niedrigerem Takt – und die letzten Watt bringen wenig Tempo. Unterhalb eines bestimmten Caps läuft aber der Rest des Servers so viel länger, dass es mehr kostet, als es spart.",
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
    "A video streaming service with its servers in Poland. It is summer: the sun makes the middays cleaner, but in the evening – when most people watch – coal carries the grid. The viewers watch on phones (40 %), laptops (25 %) and TVs (35 %). Today every device gets 4K." to
        "Ein Video-Streamingdienst mit seinen Servern in Polen. Es ist Sommer: Die Sonne macht die Mittage sauberer, aber am Abend – wenn die meisten schauen – trägt die Kohle das Netz. Geschaut wird auf Smartphones (40 %), Laptops (25 %) und Fernsehern (35 %). Heute bekommt jedes Gerät 4K.",
    "About 2 % error, which the marketing team accepts – instead of keeping every visitor ID." to
        "Rund 2 % Fehler, die das Marketing akzeptiert – statt jede Besucher-ID aufzubewahren.",
    "About Load Shift" to
        "Über Load Shift",
    "Above" to
        "Über",
    "Above the threshold every device gets at most the eco quality." to
        "Über der Schwelle bekommt jedes Gerät höchstens die Eco-Qualität.",
    "Above: CO₂ per quarter and site, electricity and hardware. Below: capacity of each site and the peak load, which grows." to
        "Oben: CO₂ je Quartal und Standort, Strom und Hardware. Unten: Kapazität je Standort und die wachsende Spitzenlast.",
    "Account balance before a transfer" to
        "Kontostand vor einer Überweisung",
    "Active: %@" to
        "Aktiv: %@",
    "Adapting the demand to the supply of clean electricity – here through the quality of the service – instead of moving it in time or place. Eco modes of apps and streaming services are the common examples; the users should notice little and be able to choose." to
        "Die Nachfrage an das Angebot an sauberem Strom anpassen – hier über die Qualität des Dienstes – statt sie zeitlich oder räumlich zu verschieben. Eco-Modi von Apps und Streamingdiensten sind die bekannten Beispiele; die Nutzer sollen wenig merken und wählen können.",
    "Add an index to the order table" to
        "Index auf die Bestelltabelle legen",
    "AI training is the most flexible load there is: it needs a lot of energy, but not at a particular hour, and it can pause at a checkpoint. Three levers: when it runs (the greenest hours of the forecast), how much power each GPU may draw (a power cap saves more energy than it costs in time), and how many servers train together (more are faster, but they spend time waiting for each other)." to
        "KI-Training ist die flexibelste Last überhaupt: Es braucht viel Energie, aber zu keiner bestimmten Stunde, und es kann an einem Checkpoint pausieren. Drei Hebel: wann es läuft (die grünsten Stunden der Prognose), wie viel Leistung jede GPU ziehen darf (ein Power Cap spart mehr Energie, als er an Zeit kostet) und wie viele Server zusammen trainieren (mehr sind schneller, verbringen aber Zeit mit Warten aufeinander).",
    "All %lld servers run all the time." to
        "Alle %lld Server laufen die ganze Zeit.",
    "All in Frankfurt, as soon as possible (reference)" to
        "Alles in Frankfurt, so bald wie möglich (Referenz)",
    "All in Frankfurt, greenest hours" to
        "Alles in Frankfurt, grünste Stunden",
    "All on" to
        "Alle an",
    "all on" to
        "alle an",
    "All servers on" to
        "Alle Server an",
    "All servers on (reference)" to
        "Alle Server an (Referenz)",
    "All servers on, 27 °C (the best PUE)" to
        "Alle Server an, 27 °C (die beste PUE)",
    "All sprints are done." to
        "Alle Sprints sind vorbei.",
    "All three levers work, and they add up. The greenest hours move the work into wind and sun; the power cap saves energy because the last watts of a GPU bring little speed (MIT Lincoln Laboratory measured 12–15 % less energy for about 3 % more time); and fewer servers waste less time waiting for each other – as long as the training still finishes in time. Each pause costs a checkpoint load: a plan that jumps between hours pays for it." to
        "Alle drei Hebel wirken, und sie addieren sich. Die grünsten Stunden verschieben die Arbeit in Wind und Sonne; der Power Cap spart Energie, weil die letzten Watt einer GPU wenig Tempo bringen (das MIT Lincoln Laboratory hat 12–15 % weniger Energie bei etwa 3 % mehr Zeit gemessen); und weniger Server verschwenden weniger Zeit mit Warten aufeinander – solange das Training noch rechtzeitig fertig wird. Jede Pause kostet das Laden eines Checkpoints: Ein Plan, der zwischen Stunden springt, bezahlt dafür.",
    "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte die Server ab, die nicht gebraucht werden – und rechtzeitig wieder ein: Ein Server braucht 30 Minuten, bis er arbeiten kann.",
    "An idle server still draws about a quarter of its maximum power. Switch off what is not needed – but a server you switch on takes 30 minutes before it can work." to
        "Ein Server im Leerlauf zieht noch etwa ein Viertel seiner Maximalleistung. Schalte ab, was nicht gebraucht wird – aber ein eingeschalteter Server braucht 30 Minuten, bis er arbeiten kann.",
    "Apply" to
        "Übernehmen",
    "arrives %@" to
        "kommt %@",
    "As allowed, 1 cache server, invalidation, HyperLogLog and materialized view" to
        "Wie erlaubt, 1 Cache-Server, Invalidierung, HyperLogLog und materialisierte Sicht",
    "As allowed, without search, 1 cache server" to
        "Wie erlaubt, ohne Suche, 1 Cache-Server",
    "As soon as possible" to
        "So früh wie möglich",
    "as soon as possible" to
        "so früh wie möglich",
    "ASHRAE recommends 18 to 27 °C at the server inlets." to
        "ASHRAE empfiehlt 18 bis 27 °C am Lufteinlass der Server.",
    "at %@" to
        "um %@ Uhr",
    "At a fixed hour" to
        "Zu einer festen Uhrzeit",
    "At least %lld servers every day from %@ to %@ (scheduled scaling)." to
        "Mindestens %1\$lld Server, jeden Tag von %2\$@ bis %3\$@ (Scheduled Scaling).",
    "at most %lld hosts running" to
        "höchstens %lld Hosts in Betrieb",
    "Autoscale" to
        "Autoscaling",
    "Autoscaling at %@" to
        "Autoscaling bei %@",
    "autoscaling at %@" to
        "Autoscaling bei %@",
    "Autoscaling at %@ (as at the start)" to
        "Autoscaling bei %@ (wie zu Beginn)",
    "autoscaling at %@, at least %lld servers %@–%@" to
        "Autoscaling bei %1\$@, mindestens %2\$lld Server %3\$@–%4\$@",
    "Autoscaling at %@, every day at least %lld servers 18:00–22:00" to
        "Autoscaling bei %1\$@, jeden Tag mindestens %2\$lld Server 18:00–22:00",
    "Autoscaling at %@, on day 2 at least %lld servers 18:00–22:00" to
        "Autoscaling bei %1\$@, an Tag 2 mindestens %2\$lld Server 18:00–22:00",
    "Autoscaling at 50 %, 20 °C (reference)" to
        "Autoscaling bei 50 %, 20 °C (Referenz)",
    "Autoscaling at 50 %, 27 °C" to
        "Autoscaling bei 50 %, 27 °C",
    "Autoscaling at 70 % (from scenario O4)" to
        "Autoscaling mit 70 % (ab Szenario B4)",
    "Autoscaling at 80 %, 20 °C" to
        "Autoscaling bei 80 %, 20 °C",
    "Autoscaling at 80 %, 25 °C" to
        "Autoscaling bei 80 %, 25 °C",
    "Autoscaling does what you did by hand in scenario O1: it switches servers on and off for the load. The target utilization decides how much headroom stays free. Little headroom saves energy – but a server takes 30 minutes to boot, and the autoscaler knows only the usual day; a sudden peak finds it unprepared. Headroom all the time costs energy all the time; a minimum for a known event costs it only when it is needed." to
        "Autoscaling macht, was du in Szenario B1 von Hand gemacht hast: Es schaltet Server passend zur Last ein und aus. Die Zielauslastung bestimmt, wie viel Reserve frei bleibt. Wenig Reserve spart Energie – aber ein Server braucht 30 Minuten zum Starten, und das Autoscaling kennt nur den gewöhnlichen Tag; eine plötzliche Spitze trifft es unvorbereitet. Reserve rund um die Uhr kostet rund um die Uhr Energie; ein Minimum für ein bekanntes Ereignis kostet sie nur, wenn sie gebraucht wird.",
    "Autumn" to
        "Herbst",
    "Back" to
        "Zurück",
    "Back to the web shop – this time to its code. Autoscaling keeps the servers in line with the load, so less CPU work per request means fewer servers. Your team has 8 story points per sprint and a backlog of ideas, each with the team's estimate of what it saves. Four sprints; each game day shows the week after a sprint." to
        "Zurück zum Webshop – diesmal zu seinem Code. Das Autoscaling hält die Server passend zur Last; weniger CPU-Arbeit pro Request heißt also weniger Server. Dein Team hat 8 Story Points pro Sprint und einen Backlog voller Ideen, jede mit der Schätzung des Teams, was sie spart. Vier Sprints; jeder Spieltag zeigt die Woche nach einem Sprint.",
    "Batch job" to
        "Batch-Job",
    "Batch jobs" to
        "Batch-Jobs",
    "Batch jobs – light: arrival to deadline, outlined: planned, filled: running or done" to
        "Batch-Jobs – hell: Ankunft bis Frist, umrandet: geplant, gefüllt: läuft oder fertig",
    "Batch rule" to
        "Batch-Regel",
    "Batch rule at both sites" to
        "Batch-Regel an beiden Standorten",
    "batch work at %lld g/kWh or less on average" to
        "Batch-Arbeit im Schnitt bei höchstens %lld g/kWh",
    "Batch work Ø %lld g/kWh (reference %lld)" to
        "Batch-Arbeit Ø %1\$lld g/kWh (Referenz %2\$lld)",
    "Batch work, mean g/kWh" to
        "Batch-Arbeit, Ø g/kWh",
    "Batch work: no job has run yet" to
        "Batch-Arbeit: noch kein Job gelaufen",
    "Batch worker" to
        "Batch-Worker",
    "batch ≤ %lld g/kWh" to
        "Batch ≤ %lld g/kWh",
    "Before the start you decide which jobs run in Oslo: the grid there is almost free of CO₂, but only %lld servers are free. A job that moves needs its data there first – the transfer takes time, and the network needs energy in the German grid." to
        "Vor dem Start entscheidest du, welche Jobs in Oslo laufen: Der Strom dort ist fast CO₂-frei, aber nur %lld Server sind frei. Ein Job, der umzieht, braucht dort zuerst seine Daten – die Übertragung dauert, und das Netz braucht Energie im deutschen Strommix.",
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
    "Bin packing is a hard problem (NP-hard): for large fleets the best placement cannot be computed quickly. Simple heuristics like First Fit Decreasing are fast, but with strict rules they leave hosts half empty, and with loose ones the peaks collide – here a careful placement by hand beats them. Real schedulers combine such heuristics with daily profiles and some headroom; spreading for safety is their default." to
        "Bin Packing ist ein schweres Problem (NP-schwer): Für große Flotten lässt sich die beste Verteilung nicht schnell berechnen. Einfache Heuristiken wie First Fit Decreasing sind schnell, lassen mit strengen Regeln aber Hosts halb leer, und mit lockeren kollidieren die Spitzen – hier schlägt eine sorgfältige Verteilung von Hand sie. Echte Scheduler verbinden solche Heuristiken mit Tagesprofilen und etwas Reserve; Verteilen aus Sicherheit ist ihre Voreinstellung.",
    "Booting" to
        "Startet",
    "booting" to
        "startet",
    "Both data centers: an orange mark shows an old server; failed servers are gone." to
        "Beide Rechenzentren: Eine orange Markierung zeigt einen alten Server; ausgefallene Server sind weg.",
    "Both sites side by side: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job." to
        "Beide Standorte nebeneinander: Graue Server sind aus, hellblaue starten, der gelbe Balken zeigt einen Server, der an einem Job arbeitet.",
    "Both trainings to Oslo" to
        "Beide Trainings nach Oslo",
    "Budget year %lld: %lld of %lld new servers left – what is not spent by the end of the year is gone." to
        "Budget Jahr %1\$lld: noch %2\$lld von %3\$lld neuen Servern – was bis zum Jahresende nicht ausgegeben ist, verfällt.",
    "Built with Kotlin and Compose Multiplatform" to
        "Gebaut mit Kotlin und Compose Multiplatform",
    "Buy when capacity runs short (reference)" to
        "Kaufen, wenn die Kapazität knapp wird (Referenz)",
    "by hand" to
        "von Hand",
    "By the saving in a year per story point" to
        "Nach der Einsparung in einem Jahr pro Story Point",
    "By the team's estimates" to
        "Nach den Schätzungen des Teams",
    "By today's saving per story point" to
        "Nach der heutigen Einsparung pro Story Point",
    "Cache memory, GB – areas: used, by kind; line: memory of the running cache servers; dotted: expected; red: stale answers" to
        "Cache-Speicher, GB – Flächen: belegt, je Art; Linie: Speicher der laufenden Cache-Server; gepunktet: erwartet; rot: veraltete Antworten",
    "Cache memory: what the running cache servers hold, what is used and what is expected (dotted). Below it the load and capacity of the web servers." to
        "Cache-Speicher: was die laufenden Cache-Server fassen, was belegt ist und was erwartet wird (gepunktet). Darunter Last und Kapazität der Web-Server.",
    "Cache server" to
        "Cache-Server",
    "Cache servers" to
        "Cache-Server",
    "Cache the catalog responses" to
        "Katalog-Antworten cachen",
    "Cache the resized images" to
        "Skalierte Bilder cachen",
    "Cache the session lookups" to
        "Session-Abfragen cachen",
    "Cache: %lld of %lld GB, %@ hits" to
        "Cache: %1\$lld von %2\$lld GB, %3\$@ Treffer",
    "Caching: the cache servers and the memory used right now, for each kind of data whether it is cached, its TTL and what the business allows, two more measures. Below it the situation right now." to
        "Caching: die Cache-Server und der gerade belegte Speicher, für jede Art von Daten, ob sie in den Cache kommt, ihre TTL und was die Fachlichkeit erlaubt, zwei weitere Maßnahmen. Darunter die Lage gerade jetzt.",
    "Campaign" to
        "Kampagne",
    "cap %@" to
        "Cap %@",
    "Capacity" to
        "Kapazität",
    "Capacity %lld, peak this quarter %lld" to
        "Kapazität %1\$lld, Spitze in diesem Quartal %2\$lld",
    "Capacity follows the load automatically: servers are added when the utilization rises above a target and removed when it falls. The target decides the headroom. Scheduled scaling adds capacity for known events in advance." to
        "Die Kapazität folgt der Last automatisch: Server kommen hinzu, wenn die Auslastung über ein Ziel steigt, und gehen, wenn sie fällt. Das Ziel bestimmt die Reserve (Headroom). Scheduled Scaling stellt für bekannte Ereignisse vorab Kapazität bereit.",
    "Capacity of the servers – red: Warsaw, green: Paris; black: peak load of a site, dotted ahead; load units" to
        "Kapazität der Server – rot: Warschau, grün: Paris; schwarz: Spitzenlast eines Standorts, gepunktet voraus; Lasteinheiten",
    "Capacity rule" to
        "Kapazitätsregel",
    "Capacity: %lld load units" to
        "Kapazität: %lld Lasteinheiten",
    "Carbon intensity of the grid at the chosen site with the forecast, and below it one row per job of that site – light: the time between arrival and deadline, filled: running or done." to
        "CO₂-Intensität des Stroms am gewählten Standort mit der Prognose, darunter eine Zeile pro Job dieses Standorts – hell: die Zeit zwischen Ankunft und Frist, gefüllt: läuft oder fertig.",
    "Carbon intensity of the grid with the forecast, the eco threshold and the hours with eco mode on (green); below it the load and capacity." to
        "CO₂-Intensität des Stroms mit Prognose, der Eco-Schwelle und den Stunden mit Eco-Modus (grün); darunter Last und Kapazität.",
    "Carbon intensity of the grid with the forecast, the load of all VMs and the capacity of the running hosts." to
        "Die CO₂-Intensität des Stroms mit Prognose, die Last aller VMs und die Kapazität der laufenden Hosts.",
    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted). The campaign is marked – but it is not in the expected load." to
        "Die CO₂-Intensität des Stroms mit Prognose, die Last bisher und die erwartete Last (gepunktet). Die Kampagne ist markiert – aber nicht in der erwarteten Last.",
    "Carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs." to
        "Die CO₂-Intensität des Stroms mit Prognose, die Last bisher und die erwartete Last (gepunktet); die Linien rechts zeigen, wie viele Server eine Last braucht.",
    "Carbon intensity of the grid with the forecast; below it when the GPUs train and the progress, ahead the plan with the rules now." to
        "CO₂-Intensität des Stroms mit Prognose; darunter, wann die GPUs trainieren, und der Fortschritt, voraus der Plan mit den aktuellen Regeln.",
    "Carbon intensity of the grid, g CO₂/kWh – dashed: forecast" to
        "CO₂-Intensität des Stroms, g CO₂/kWh – gestrichelt: Prognose",
    "Carbon intensity of the grid, g CO₂/kWh – dashed: forecast; green: eco mode on" to
        "CO₂-Intensität des Stroms, g CO₂/kWh – gestrichelt: Prognose; grün: Eco-Modus an",
    "Catalog pages" to
        "Katalogseiten",
    "chiller" to
        "Kältemaschine",
    "chiller %@, outside air" to
        "Kältemaschine %@, Außenluft",
    "Chiller: %@" to
        "Kältemaschine: %@",
    "compared with the reference operator, who buys new servers wherever capacity runs short." to
        "im Vergleich zum Referenzbetreiber, der neue Server kauft, wo die Kapazität knapp wird.",
    "compared with the reference operator, who caches nothing." to
        "im Vergleich zum Referenzbetreiber, der nichts cacht.",
    "compared with the reference operator, who keeps the VMs spread over all ten hosts." to
        "im Vergleich zum Referenzbetreiber, der die VMs auf alle zehn Hosts verteilt lässt.",
    "compared with the reference operator, who leaves every server on." to
        "im Vergleich zum Referenzbetreiber, der alle Server laufen lässt.",
    "compared with the reference operator, who runs every job in Frankfurt as soon as it arrives." to
        "verglichen mit dem Referenzbetreiber, der jeden Job in Frankfurt startet, sobald er ankommt.",
    "compared with the reference operator, who starts every job as soon as it arrives." to
        "im Vergleich zum Referenzbetreiber, der jeden Job sofort bei Ankunft startet.",
    "compared with the reference operator, who streams 4K to every device." to
        "im Vergleich zum Referenzbetreiber, der jedem Gerät 4K liefert.",
    "compared with the reference operator, whose team changes nothing in the code." to
        "im Vergleich zum Referenzbetreiber, dessen Team nichts am Code ändert.",
    "compared with the reference operator: autoscaling at 50 %, setpoint 20 °C." to
        "im Vergleich zum Referenzbetreiber: Autoscaling bei 50 %, Sollwert 20 °C.",
    "compared with the reference operator: eight servers, no cap, at once." to
        "im Vergleich zum Referenzbetreiber: acht Server, kein Cap, sofort.",
    "Consolidation" to
        "Konsolidierung",
    "Cooled by" to
        "Gekühlt mit",
    "Cooling" to
        "Kühlung",
    "Cooling is the biggest part of the facility's own power. Outside air cools for free when it is cooler than the server inlets; in a heat wave the chiller does the work, and the hotter it is outside, the more power it needs. Servers tolerate warmer air than most data centers give them – up to 27 °C – but above 25 °C their fans speed up. And the PUE, facility power divided by IT power, does not always point the right way." to
        "Die Kühlung ist der größte Teil des Eigenverbrauchs eines Rechenzentrums. Außenluft kühlt kostenlos, wenn sie kühler ist als die Luft am Servereinlass; in einer Hitzewelle arbeitet die Kältemaschine, und je heißer es draußen ist, desto mehr Strom braucht sie. Server vertragen wärmere Luft, als die meisten Rechenzentren ihnen geben – bis 27 °C –, aber über 25 °C drehen ihre Lüfter hoch. Und die PUE, die Leistung des Rechenzentrums geteilt durch die IT-Leistung, zeigt nicht immer in die richtige Richtung.",
    "Cooling: the setpoint and what cools right now. Below it the capacity rule and the situation right now." to
        "Kühlung: der Sollwert und womit gerade gekühlt wird. Darunter die Kapazitätsregel und die Lage gerade jetzt.",
    "Count unique visitors with HyperLogLog" to
        "Eindeutige Besucher mit HyperLogLog zählen",
    "CO₂ %@ or better" to
        "CO₂ %@ oder besser",
    "CO₂ from %@" to
        "CO₂ ab %@",
    "CO₂ per quarter, t – dark: electricity, light: hardware; red: Warsaw, green: Paris; black line: reference" to
        "CO₂ je Quartal, t – dunkel: Strom, hell: Hardware; rot: Warschau, grün: Paris; schwarzer Strich: Referenz",
    "CO₂ so far" to
        "CO₂ bisher",
    "CO₂ vs. reference" to
        "CO₂ ggü. Referenz",
    "CO₂ vs. reference: %@" to
        "CO₂ ggü. Referenz: %@",
    "CO₂ vs. you" to
        "CO₂ ggü. dir",
    "CPU time per request" to
        "CPU-Zeit pro Request",
    "CPU work of a day" to
        "CPU-Arbeit eines Tages",
    "Customer analytics" to
        "Kundenanalyse",
    "Customer data moved abroad" to
        "Kundendaten ins Ausland verschoben",
    "Customer data stays in Germany" to
        "Kundendaten bleiben in Deutschland",
    "Customer data: by contract it must stay in Germany." to
        "Kundendaten: Laut Vertrag müssen sie in Deutschland bleiben.",
    "Daily report" to
        "Tagesbericht",
    "Data import" to
        "Datenimport",
    "Data transfers (kWh)" to
        "Datenübertragungen (kWh)",
    "Data: %@ TB" to
        "Daten: %@ TB",
    "Data: %@ × the start, growing %@ a month." to
        "Daten: %1\$@ × der Anfang, Wachstum %2\$@ pro Monat.",
    "Database replica" to
        "Datenbank-Replikat",
    "Database: N+1 queries in the product list" to
        "Datenbank: N+1-Abfragen in der Produktliste",
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
    "done in sprint %lld" to
        "erledigt in Sprint %lld",
    "Each game day stands for a month, with one sprint at its start. The team has %lld story points per sprint for performance – the rest goes into features. Items bigger than a sprint carry over; started items are continued first." to
        "Jeder Spieltag steht für einen Monat mit einem Sprint am Anfang. Das Team hat %lld Story Points pro Sprint für Performance – der Rest geht in Features. Größere Punkte laufen über mehrere Sprints; Begonnenes wird zuerst fortgesetzt.",
    "Each game day stands for the week after a sprint. Your team has %lld story points per sprint. Pick items from the backlog and start the sprint – their effect counts from this day on. Items that do not fit carry over into the next sprint." to
        "Jeder Spieltag steht für die Woche nach einem Sprint. Dein Team hat %lld Story Points pro Sprint. Wähle Punkte aus dem Backlog und starte den Sprint – ihre Wirkung zählt ab diesem Tag. Was nicht mehr passt, läuft in den nächsten Sprint weiter.",
    "Each GPU server draws %@ kW while training and works at %@ of full speed." to
        "Jeder GPU-Server zieht beim Training %1\$@ kW und arbeitet mit %2\$@ des vollen Tempos.",
    "Each host: %lld GB RAM, %lld load units." to
        "Jeder Host: %1\$lld GB RAM, %2\$lld Lasteinheiten.",
    "Each job needs servers for some hours and must be finished by its deadline. When it starts, its servers switch on and boot for 30 minutes first; servers without a job are off." to
        "Jeder Job braucht einige Stunden lang Server und muss bis zu seiner Frist fertig sein. Beim Start schalten sich seine Server ein und starten zuerst 30 Minuten lang; Server ohne Job sind aus.",
    "Earn a star in scenario %@ first." to
        "Hol dir zuerst einen Stern in Szenario %@.",
    "eco above %lld" to
        "Eco über %lld",
    "Eco mode" to
        "Eco-Modus",
    "Eco mode while the grid is dirty" to
        "Eco-Modus bei schmutzigem Strom",
    "Eco quality" to
        "Eco-Qualität",
    "Energy (kWh)" to
        "Energie (kWh)",
    "Energy (MWh)" to
        "Energie (MWh)",
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
    "every job by its deadline, customer data in Germany and less CO₂ than the reference" to
        "jeder Job bis zu seiner Frist, Kundendaten in Deutschland und weniger CO₂ als die Referenz",
    "Every job is planned into the hours with the lowest forecast before its deadline, once, when you apply the rule." to
        "Jeder Job wird in die Stunden mit der niedrigsten Prognose vor seiner Frist geplant – einmal, wenn du die Regel übernimmst.",
    "Every job starts at the next %@ after it arrives." to
        "Jeder Job startet nach seiner Ankunft beim nächsten Mal um %@ Uhr.",
    "Every job starts when it arrives." to
        "Jeder Job startet, sobald er da ist.",
    "Every server runs all the time – like the reference operator." to
        "Alle Server laufen ständig – wie beim Referenzbetreiber.",
    "Everything allowed, search too, 4 cache servers (the team's plan)" to
        "Alles wie erlaubt, auch die Suche, 4 Cache-Server (Plan des Teams)",
    "Exchange rates" to
        "Wechselkurse",
    "Expected load %lld" to
        "Erwartete Last %lld",
    "Expected load in 1 h" to
        "Erwartete Last in 1 h",
    "Facility energy divided by IT energy: 1.0 would mean that all power goes to the servers. The worldwide average is about 1.5, hyperscalers reach 1.1. A warmer setpoint and cooling with outside air (free cooling) lower it." to
        "Energie des Rechenzentrums geteilt durch die Energie der IT: 1,0 hieße, aller Strom geht an die Server. Weltweit liegt der Schnitt bei etwa 1,5, Hyperscaler erreichen 1,1. Ein wärmerer Sollwert und Kühlen mit Außenluft (Free Cooling) senken sie.",
    "faster by a constant factor" to
        "schneller um einen konstanten Faktor",
    "Faster string handling in the templates" to
        "Schnellere String-Verarbeitung in den Templates",
    "Fewer hosts, less idle power: pack the VMs tighter. Two things decide whether a VM fits: its RAM and its CPU over the day. By RAM alone five hosts would do – but then the peaks collide. VMs whose peaks come at different times – batch at night, reporting in the morning, the shop in the evening – fit together; VMs that peak at the same time do not." to
        "Weniger Hosts, weniger Leerlaufstrom: Packe die VMs dichter. Zwei Dinge entscheiden, ob eine VM passt: ihr RAM und ihre CPU über den Tag. Nach RAM allein würden fünf Hosts reichen – aber dann kollidieren die Spitzen. VMs, deren Spitzen zu verschiedenen Zeiten kommen – Batch nachts, Reporting morgens, der Shop abends –, passen zusammen; VMs mit gleichzeitiger Spitze nicht.",
    "First Fit Decreasing, daily profiles up to 100 %" to
        "First Fit Decreasing, Tagesprofile bis 100 %",
    "First Fit Decreasing, daily profiles up to 85 %" to
        "First Fit Decreasing, Tagesprofile bis 85 %",
    "First Fit Decreasing, RAM only" to
        "First Fit Decreasing, nur RAM",
    "First Fit Decreasing, sum of the peaks" to
        "First Fit Decreasing, Summe der Spitzen",
    "Fix the N+1 queries in the product list" to
        "N+1-Abfragen in der Produktliste beheben",
    "Fixed" to
        "Fest",
    "fixed %lld servers" to
        "fest %lld Server",
    "Fixed number" to
        "Feste Anzahl",
    "Fixed: %lld servers all day" to
        "Fest: %lld Server den ganzen Tag",
    "Font: Inter, SIL Open Font License 1.1" to
        "Schrift: Inter, SIL Open Font License 1.1",
    "For courses that take the topics in another order. Stars still count." to
        "Für Lehrveranstaltungen, die die Themen in anderer Reihenfolge behandeln. Sterne zählen weiter.",
    "For teachers" to
        "Für Lehrende",
    "Forecast update: the wind drops about 12 hours earlier than forecast." to
        "Neue Prognose: Der Wind lässt etwa 12 Stunden früher nach als vorhergesagt.",
    "Forecast while it runs: %lld g/kWh · start by %@" to
        "Prognose während der Laufzeit: %1\$lld g/kWh · spätestens %2\$@",
    "Forecast: %lld g/kWh, ±%lld %%" to
        "Prognose: %1\$lld g/kWh, ±%2\$lld %%",
    "Framework and the rest" to
        "Framework und der Rest",
    "France" to
        "Frankreich",
    "from %@" to
        "ab %@",
    "From 03:00 to 07:00 computes all catalog pages in all variants (language, currency, sort order) in advance, so that nothing is missing in the morning." to
        "Berechnet von 03:00 bis 07:00 alle Katalogseiten in allen Varianten (Sprache, Währung, Sortierung) vorab, damit morgens nichts fehlt.",
    "Full: entries are pushed out before their TTL ends." to
        "Voll: Einträge werden verdrängt, bevor ihre TTL abläuft.",
    "Germany" to
        "Deutschland",
    "Goals" to
        "Ziele",
    "GPU servers" to
        "GPU-Server",
    "Great Britain" to
        "Großbritannien",
    "Green IT – from code to data center" to
        "Green IT – vom Code bis zum Rechenzentrum",
    "greenest hours" to
        "grünste Stunden",
    "Greenest hours" to
        "Grünste Stunden",
    "Greenest hours (forecast)" to
        "Sauberste Stunden (Prognose)",
    "Greenest hours, planned again after the forecast update" to
        "Sauberste Stunden, nach der neuen Prognose neu geplant",
    "Greenest hours, planned once" to
        "Sauberste Stunden, einmal geplant",
    "Grid data: Electricity Maps, Grid Review 2025 (life-cycle, imports included)" to
        "Netzdaten: Electricity Maps, Grid Review 2025 (Lebenszyklus, Importe eingerechnet)",
    "Grid now" to
        "Netz jetzt",
    "grid on average %lld g/kWh" to
        "Strommix im Mittel %lld g/kWh",
    "Grid: %lld g/kWh" to
        "Strom: %lld g/kWh",
    "Growing data" to
        "Wachsende Daten",
    "Hardware renewal" to
        "Hardware-Erneuerung",
    "Hardware renewal: the year's budget, the generation on offer, and per site its servers, capacity against the peak load, CO₂ so far, and how many new servers it gets." to
        "Hardware-Erneuerung: das Budget des Jahres, die angebotene Generation und je Standort die Server, die Kapazität gegen die Spitzenlast, das CO₂ bisher und wie viele neue Server er bekommt.",
    "HD everywhere" to
        "Überall HD",
    "HD on phones and laptops, 4K on TVs" to
        "HD auf Smartphones und Laptops, 4K auf Fernsehern",
    "Headroom is insurance: it costs energy every hour, whether the peak comes or not. When a peak is known in advance, scheduled scaling is cheaper – cloud platforms offer it next to the autoscaler. Predictive autoscaling goes one step further and learns such peaks from the past. Part of the 30 minutes is the software itself: a Java service often needs a while to start and warm up. GraalVM Native Image or CRaC (Coordinated Restore at Checkpoint) start it in a fraction of the time – the faster a server is ready, the less headroom the autoscaler needs." to
        "Reserve ist eine Versicherung: Sie kostet jede Stunde Energie, ob die Spitze kommt oder nicht. Ist eine Spitze vorher bekannt, ist Scheduled Scaling günstiger – Cloud-Plattformen bieten das neben dem Autoscaling an. Predictive Autoscaling geht einen Schritt weiter und lernt solche Spitzen aus der Vergangenheit. Ein Teil der 30 Minuten ist die Software selbst: Ein Java-Dienst braucht oft eine Weile, bis er gestartet und warmgelaufen ist. GraalVM Native Image oder CRaC (Coordinated Restore at Checkpoint) starten ihn in einem Bruchteil der Zeit – je schneller ein Server bereit ist, desto weniger Reserve braucht das Autoscaling.",
    "Heat wave" to
        "Hitzewelle",
    "Hit or miss" to
        "Treffer oder nicht",
    "hits now: %@ · %lld GB" to
        "Treffer jetzt: %1\$@ · %2\$lld GB",
    "Host %lld: expected peak %@ of %lld load units – above %@ requests get slow." to
        "Host %1\$lld: erwartete Spitze %2\$@ von %3\$lld Lasteinheiten – über %4\$@ werden Requests langsam.",
    "Hosts in use" to
        "Hosts in Benutzung",
    "Hosts in use, RAM, warnings for hosts that will be too busy, the VM kinds with their daily profiles." to
        "Hosts in Benutzung, RAM, Warnungen für Hosts, die zu voll werden, die VM-Arten mit ihren Tagesprofilen.",
    "How the work of an algorithm grows with the amount of data n, written in Big O notation: O(1) stays the same, O(n) grows in step with the data, O(n²) with its square. For small data the difference hardly shows; for growing data it decides." to
        "Wie die Arbeit eines Algorithmus mit der Datenmenge n wächst, geschrieben in O-Notation: O(1) bleibt gleich, O(n) wächst im Gleichschritt mit den Daten, O(n²) mit ihrem Quadrat. Bei kleinen Daten fällt der Unterschied kaum auf; bei wachsenden Daten entscheidet er.",
    "Image conversion" to
        "Bildkonvertierung",
    "Image recognition training" to
        "Training Bilderkennung",
    "Image resizing on every request" to
        "Bildskalierung bei jedem Request",
    "in progress: %lld of %lld story points left" to
        "in Arbeit: noch %1\$lld von %2\$lld Story Points",
    "In time: %@" to
        "Rechtzeitig: %@",
    "In Warsaw a new server saves many times its embodied emissions; in Paris it saves little, because the electricity is almost free of CO₂ – there it only has to carry the growth and replace the failed servers, and later generations do that better. The question is not “new or old?” but “where does new hardware replace dirty electricity?”. The earlier the budget goes to the dirty grid, the more it saves – as long as the clean site keeps enough capacity." to
        "In Warschau spart ein neuer Server ein Vielfaches seiner Herstellungsemissionen; in Paris spart er wenig, weil der Strom fast frei von CO₂ ist – dort muss er nur das Wachstum tragen und die ausgefallenen Server ersetzen, und das können spätere Generationen besser. Die Frage ist nicht „neu oder alt?“, sondern „wo ersetzt neue Hardware schmutzigen Strom?“. Je früher das Budget in den schmutzigen Strommix geht, desto mehr spart es – solange der saubere Standort genug Kapazität behält.",
    "Invalidate the catalog pages when the prices change" to
        "Katalogseiten bei Preisänderung invalidieren",
    "invalidation" to
        "Invalidierung",
    "IT power" to
        "IT-Leistung",
    "Jobs done" to
        "Jobs fertig",
    "Jobs on time" to
        "Jobs rechtzeitig",
    "JSON for the product catalog, built on every request" to
        "JSON für den Produktkatalog, bei jedem Request neu gebaut",
    "Keeping results for a while so that they need not be computed or fetched again. The time to live (TTL) says how long an entry counts, the hit rate how often a request finds its answer in the cache. A cache trades memory and freshness for computation." to
        "Ergebnisse eine Zeit lang aufbewahren, damit sie nicht erneut berechnet oder geholt werden müssen. Die Time to Live (TTL) sagt, wie lange ein Eintrag gilt, die Trefferquote, wie oft eine Anfrage ihre Antwort im Cache findet. Ein Cache tauscht Speicher und Aktualität gegen Rechenarbeit.",
    "Keeps enough servers on for the expected load of the next hour – or the measured load, if it is higher – at %@ utilization. The rest is headroom." to
        "Hält genug Server für die erwartete Last der nächsten Stunde – oder die gemessene, falls sie höher ist – bei %@ Auslastung. Der Rest ist Reserve.",
    "late, %@" to
        "zu spät, %@",
    "Lazy-load the product images" to
        "Produktbilder erst beim Scrollen laden",
    "Load %lld, capacity %lld (%lld servers on)" to
        "Last %1\$lld, Kapazität %2\$lld (%3\$lld Server an)",
    "Load (line) and capacity of the running servers (area) – red: slow requests" to
        "Last (Linie) und Kapazität der laufenden Server (Fläche) – rot: langsame Requests",
    "Load / capacity" to
        "Last / Kapazität",
    "Location" to
        "Standort",
    "Location saves more than time here: no hour in the German grid comes close to Norway's hydropower. Look at the energy: it rose – the transfers need energy too – and still the CO₂ fell. Location Shifting saves carbon, not energy. The trainings were the best candidates – hours of computing for little data. The videos were the worst: 30 terabytes through the network cost more CO₂ than Oslo saved, and the transfer took most of a working day. The price calculation needs its database in Frankfurt and misses its deadline far from it; customer data may not leave the country by contract. How much energy a terabyte in the network needs is uncertain – estimates differ by a factor of ten and more [S24] – but the lesson stays: data have weight. And clean electricity is not unlimited: if everyone moves to Norway, it runs short there. Location Shifting works best with capacity that is there anyway." to
        "Der Ort spart hier mehr als die Zeit: Keine Stunde im deutschen Netz kommt an Norwegens Wasserkraft heran. Schau auf die Energie: Sie stieg – auch die Übertragungen brauchen Energie –, und trotzdem sank das CO₂. Location Shifting spart CO₂, nicht Energie. Die Trainings waren die besten Kandidaten – Stunden Rechnen für wenig Daten. Die Videos waren die schlechtesten: 30 Terabyte durchs Netz kosten mehr CO₂, als Oslo spart, und die Übertragung dauert fast einen Arbeitstag. Die Preisberechnung braucht ihre Datenbank in Frankfurt und verpasst fern von ihr die Frist; Kundendaten dürfen laut Vertrag das Land nicht verlassen. Wie viel Energie ein Terabyte im Netz braucht, ist unsicher – Schätzungen liegen um den Faktor zehn und mehr auseinander [S24] –, aber die Lehre bleibt: Daten haben Gewicht. Und sauberer Strom ist nicht unbegrenzt: Ziehen alle nach Norwegen, wird er dort knapp. Location Shifting wirkt am besten mit Kapazität, die ohnehin da ist.",
    "Location Shifting: let the work run where the electricity is clean. But work does not travel alone – its data must go along. Moving terabytes takes hours and energy in the network; a job that talks to a database all the time becomes slow far away from it; and some data may not leave the country at all. Move what needs much computing and little data." to
        "Location Shifting: Lass die Arbeit dort laufen, wo der Strom sauber ist. Aber Arbeit reist nicht allein – ihre Daten müssen mit. Terabytes zu verschieben dauert Stunden und kostet Energie im Netz; ein Job, der ständig mit einer Datenbank spricht, wird fern von ihr langsam; und manche Daten dürfen das Land gar nicht verlassen. Verschiebe, was viel rechnet und wenig Daten hat.",
    "Location: for every job its servers, duration, time window and data – and where it runs. Decide before the start; above it the batch rule for both sites." to
        "Standort: für jeden Job seine Server, Dauer, Zeitfenster und Daten – und wo er läuft. Entscheide vor dem Start; darüber die Batch-Regel für beide Standorte.",
    "Log at INFO instead of DEBUG in production" to
        "In Produktion auf INFO statt DEBUG loggen",
    "Logging at DEBUG level" to
        "Logging auf DEBUG-Level",
    "Longer than allowed: some answers will be stale." to
        "Länger als erlaubt: Manche Antworten werden veraltet sein.",
    "materialized view" to
        "materialisierte Sicht",
    "Materialized view for the sales dashboard" to
        "Materialisierte Sicht für das Umsatz-Dashboard",
    "May be up to %@ old." to
        "Darf bis zu %@ alt sein.",
    "May be up to a day old." to
        "Darf bis zu einem Tag alt sein.",
    "Mean utilization" to
        "Mittlere Auslastung",
    "mean utilization at least %@" to
        "mittlere Auslastung mindestens %@",
    "Measure before you optimize: the big items on the team's list hardly mattered, while a one-line change of the log level saved a tenth of the CPU. The checkout is Amdahl's law at work: however much faster a part gets, the whole gains at most its share. Less work per request lowers the energy and – with autoscaling – the number of servers; a data center that needs fewer servers also needs less hardware. The SCI per 1,000 requests shows the effect independent of the load." to
        "Erst messen, dann optimieren: Die großen Punkte auf der Liste des Teams brachten kaum etwas, eine einzige Zeile beim Log-Level dagegen ein Zehntel der CPU. Der Checkout zeigt Amdahls Gesetz: Wie viel schneller ein Teil auch wird, das Ganze gewinnt höchstens seinen Anteil. Weniger Arbeit pro Request senkt die Energie und – mit Autoscaling – die Zahl der Server; ein Rechenzentrum, das weniger Server braucht, braucht auch weniger Hardware. Die SCI pro 1.000 Requests zeigt den Effekt unabhängig von der Last.",
    "Measure first" to
        "Erst messen",
    "Measured today, some parts of the code hardly matter: a lookup in a list, a query without an index, a nightly job that compares every pair of products. But their work grows with the data – in step with it, or even with its square. Constant savings look bigger today; fixing the parts that grow pays off over the year. The complexity of an algorithm tells you how its work grows." to
        "Heute gemessen spielen manche Teile des Codes kaum eine Rolle: eine Suche in einer Liste, eine Abfrage ohne Index, ein nächtlicher Job, der jedes Produktpaar vergleicht. Doch ihre Arbeit wächst mit den Daten – im Gleichschritt oder sogar mit dem Quadrat. Konstante Einsparungen sehen heute größer aus; über das Jahr zahlt es sich aus, die wachsenden Teile zu beheben. Die Komplexität eines Algorithmus sagt, wie seine Arbeit wächst.",
    "measured: −%@" to
        "gemessen: −%@",
    "measures where the time goes" to
        "misst, wohin die Zeit geht",
    "Measuring where a program spends its time and memory, function by function. A profiler shows the hot spots – often not where the team expects them. Without measuring, optimization is guesswork (premature optimization)." to
        "Messen, wo ein Programm seine Zeit und seinen Speicher verbraucht, Funktion für Funktion. Ein Profiler zeigt die Hotspots – oft nicht dort, wo das Team sie vermutet. Ohne Messung ist Optimieren Raten (Premature Optimization).",
    "Memory now: %lld of %lld GB used" to
        "Speicher jetzt: %1\$lld von %2\$lld GB belegt",
    "Minimum in a time window" to
        "Minimum in einem Zeitfenster",
    "Missed" to
        "Verpasst",
    "MIT License" to
        "MIT-Lizenz",
    "More measures" to
        "Weitere Maßnahmen",
    "Most of the saving needed no compromise: 4K on a phone is work nobody sees. The eco mode adds the rest – in the dirty evening hours, where it saves most and where it is noticed most. SD went too far: the viewers' rating fell below the target. In reality most of the energy of streaming goes into networks and devices, not into the data center [S8]; a lower bitrate saves there as well." to
        "Der größte Teil der Einsparung kostete nichts: 4K auf einem Smartphone ist Arbeit, die niemand sieht. Der Eco-Modus bringt den Rest – in den schmutzigen Abendstunden, wo er am meisten spart und am meisten auffällt. SD ging zu weit: Die Bewertung der Zuschauer fiel unter das Ziel. In Wirklichkeit geht der größte Teil der Energie beim Streaming in Netze und Geräte, nicht ins Rechenzentrum [S8]; eine niedrigere Bitrate spart auch dort.",
    "Moving flexible work into the hours when the grid is cleanest. Together with Location Shifting it is called Demand Shifting – part of the Green Software Foundation principle Carbon Awareness." to
        "Flexible Arbeit in die Stunden verschieben, in denen der Strom am saubersten ist. Zusammen mit Location Shifting heißt das Demand Shifting – ein Teil des Prinzips Carbon Awareness der Green Software Foundation.",
    "Moving flexible work to the place where the electricity is cleanest. Together with Time Shifting it is called Demand Shifting – part of the Green Software Foundation principle Carbon Awareness. Its limits: moving the data, the latency to users and other systems, and where data may be stored." to
        "Flexible Arbeit an den Ort verschieben, an dem der Strom am saubersten ist. Zusammen mit Time Shifting heißt das Demand Shifting – ein Teil des Prinzips Carbon Awareness der Green Software Foundation. Seine Grenzen: das Verschieben der Daten, die Latenz zu Nutzern und anderen Systemen und wo Daten gespeichert werden dürfen.",
    "Must be current." to
        "Muss aktuell sein.",
    "New hardware does more work per kilowatt-hour – a new server does the work of two old ones. But making a server emits CO₂ too, about 550 kg. Whether a new server pays off depends on the grid: where electricity is dirty, the energy it saves outweighs its making within weeks; where it is clean, hardly ever. A site in a clean grid needs new servers only when its capacity runs short – and then preferably the newest generation." to
        "Neue Hardware schafft mehr Arbeit pro Kilowattstunde – ein neuer Server leistet so viel wie zwei alte. Aber auch die Herstellung eines Servers verursacht CO₂, etwa 550 kg. Ob sich ein neuer Server lohnt, hängt am Strommix: Wo der Strom schmutzig ist, wiegt die gesparte Energie seine Herstellung in Wochen auf; wo er sauber ist, kaum je. Ein Standort mit sauberem Strom braucht neue Server erst, wenn die Kapazität knapp wird – und dann am besten die neueste Generation.",
    "New rule" to
        "Neue Regel",
    "New servers" to
        "Neue Server",
    "New servers arrive at the start of the next quarter. A new server does the work of two old ones; making it emits about 550 kg CO₂, booked over its six years. Old servers past six years begin to fail. The shop grows by 15 % a year." to
        "Neue Server kommen zu Beginn des nächsten Quartals. Ein neuer Server leistet so viel wie zwei alte; seine Herstellung verursacht etwa 550 kg CO₂, verteilt über seine sechs Jahre. Alte Server über sechs Jahre fallen nach und nach aus. Der Shop wächst um 15 % pro Jahr.",
    "Night shift" to
        "Nachtschicht",
    "no cache" to
        "kein Cache",
    "No cache (reference)" to
        "Kein Cache (Referenz)",
    "no cap" to
        "kein Cap",
    "No deadline missed" to
        "Keine Frist verpasst",
    "No job" to
        "Kein Job",
    "none" to
        "keiner",
    "Norway" to
        "Norwegen",
    "Not applied yet – active: %@" to
        "Noch nicht übernommen – aktiv: %@",
    "not done" to
        "nicht fertig",
    "Not made" to
        "Nicht geschafft",
    "not planned" to
        "nicht geplant",
    "Now %@ of the original." to
        "Jetzt %@ des ursprünglichen Werts.",
    "O%@" to
        "B%@",
    "of %lld servers on" to
        "von %lld Servern an",
    "of it embodied (kg)" to
        "davon Herstellung (kg)",
    "of it embodied (t)" to
        "davon Herstellung (t)",
    "Off" to
        "Aus",
    "off" to
        "aus",
    "Old iron" to
        "Altes Eisen",
    "Old server" to
        "Alter Server",
    "Old servers failed" to
        "Ausgefallene alte Server",
    "on" to
        "an",
    "On the screen" to
        "Auf dem Bildschirm",
    "One server handles %lld load units (%lld,000 requests per 15 minutes). Above 85 %% utilization requests get slow." to
        "Ein Server schafft %1\$lld Lasteinheiten (%2\$lld.000 Requests pro 15 Minuten). Über 85 %% Auslastung werden Requests langsam.",
    "One star opens the next scenario." to
        "Ein Stern schaltet das nächste Szenario frei.",
    "Open all scenarios" to
        "Alle Szenarien freigeben",
    "Operations" to
        "Betrieb",
    "Optimizing by gut feeling is a gamble: the team expects the most from the image resizing and from rewriting the checkout. Whether that is true, only a measurement shows. Profiling costs a few story points and shows where the CPU time really goes – then you fix the hot spots first." to
        "Optimieren nach Bauchgefühl ist ein Glücksspiel: Das Team erwartet am meisten vom Bildskalieren und vom Neuschreiben des Checkouts. Ob das stimmt, zeigt nur eine Messung. Profiling kostet ein paar Story Points und zeigt, wohin die CPU-Zeit wirklich geht – dann behebst du zuerst die Hotspots.",
    "Order history without an index" to
        "Bestellhistorie ohne Index",
    "Outside" to
        "Außen",
    "Outside %@ °C, setpoint %lld °C" to
        "Außen %1\$@ °C, Sollwert %2\$lld °C",
    "outside air" to
        "Außenluft",
    "Outside temperature with the forecast and the setpoint; green: hours in which the outside air cools. Below it the load and capacity." to
        "Außentemperatur mit Vorhersage und Sollwert; grün: Stunden, in denen die Außenluft kühlt. Darunter Last und Kapazität.",
    "Outside temperature, °C – dashed: forecast; black: setpoint; green: the outside air cools" to
        "Außentemperatur, °C – gestrichelt: Vorhersage; schwarz: Sollwert; grün: die Außenluft kühlt",
    "Pages of about 400 KB; most searches are typed only once." to
        "Seiten mit je etwa 400 KB; die meisten Suchen gibt es nur einmal.",
    "Paris first" to
        "Erst Paris",
    "peak %@" to
        "Spitze %@",
    "Phones" to
        "Smartphones",
    "Picked up: %@ (%lld GB). Now click a host." to
        "Aufgenommen: %1\$@ (%2\$lld GB). Jetzt einen Host anklicken.",
    "Placement" to
        "Platzierung",
    "Plan" to
        "Planen",
    "Plan again with the latest forecast" to
        "Mit neuester Prognose neu planen",
    "planned" to
        "geplant",
    "planned %@" to
        "geplant %@",
    "Planned by the forecast (from scenario O3)" to
        "Nach Prognose geplant (ab Szenario B3)",
    "Planned with the forecast of %@." to
        "Geplant mit der Prognose von %@.",
    "Planned: %@" to
        "Geplant: %@",
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
    "Power cap" to
        "Power Cap",
    "Precompute the shipping costs" to
        "Versandkosten vorab berechnen",
    "Price calculation" to
        "Preisberechnung",
    "Prices change" to
        "Preise ändern sich",
    "Product images" to
        "Produktbilder",
    "Product lookup in a list" to
        "Produktsuche in einer Liste",
    "Product video encoding" to
        "Encoding Produktvideos",
    "Profile the shop" to
        "Den Shop profilen",
    "Profiling first, then by the measurements" to
        "Erst Profiling, dann nach den Messwerten",
    "Progress: %@ · resumed %lld times" to
        "Fortschritt: %1\$@ · %2\$lld-mal fortgesetzt",
    "PUE now" to
        "PUE jetzt",
    "Q%lld/%lld" to
        "Q%1\$lld/%2\$lld",
    "Quarter %lld, year %lld" to
        "Quartal %1\$lld, Jahr %2\$lld",
    "RAM in use" to
        "RAM belegt",
    "Rating" to
        "Bewertung",
    "Rating now" to
        "Bewertung jetzt",
    "Read from the core banking system." to
        "Aus dem Kernbankensystem gelesen.",
    "Recommendation model training" to
        "Training Empfehlungsmodell",
    "Recommendations" to
        "Empfehlungen",
    "Reference" to
        "Referenz",
    "Remove the plan" to
        "Plan entfernen",
    "Removes them from the cache at once (day 2, 18:00); the cache fills again. Without it, cached pages show the old prices until they expire." to
        "Entfernt sie sofort aus dem Cache (Tag 2, 18:00); der Cache füllt sich neu. Ohne das zeigen gecachte Seiten die alten Preise, bis sie ablaufen.",
    "Resumed after a pause" to
        "Nach Pause fortgesetzt",
    "Reuse TLS sessions" to
        "TLS-Sessions wiederverwenden",
    "Rewrite the checkout in a compiled language" to
        "Checkout in einer kompilierten Sprache neu schreiben",
    "Rewrite the image service in a compiled language" to
        "Bild-Service in einer kompilierten Sprache neu schreiben",
    "Right now" to
        "Gerade jetzt",
    "Rule" to
        "Regel",
    "Run" to
        "Start",
    "Running only as much capacity as the load needs. An idle server still draws power, so servers that are not needed are switched off. Green Software Foundation principle: energy efficiency." to
        "Nur so viel Kapazität betreiben, wie die Last braucht. Ein Server im Leerlauf zieht trotzdem Strom, deshalb werden nicht benötigte Server abgeschaltet. Prinzip der Green Software Foundation: Energy Efficiency.",
    "Running the work on as few machines as it needs, so the others can be switched off. Placing items of different sizes into as few bins as possible is called bin packing." to
        "Die Arbeit auf so wenigen Maschinen laufen lassen, wie sie braucht, damit die anderen abgeschaltet werden können. Dinge verschiedener Größe in möglichst wenige Behälter zu legen, heißt Bin Packing.",
    "Running: %@" to
        "Läuft: %@",
    "runs until %@" to
        "läuft bis %@",
    "Scenario %@: %@" to
        "Szenario %1\$@: %2\$@",
    "Scenario complete" to
        "Szenario geschafft",
    "Search index" to
        "Suchindex",
    "Search index, rebuilt every night" to
        "Suchindex, jede Nacht neu gebaut",
    "Search results" to
        "Suchergebnisse",
    "Servers in use: %lld of %lld" to
        "Server in Benutzung: %1\$lld von %2\$lld",
    "Servers on (mean)" to
        "Server an (Mittel)",
    "Servers working" to
        "Server in Betrieb",
    "Sessions and encryption" to
        "Sessions und Verschlüsselung",
    "Setpoint" to
        "Sollwert",
    "Shop front end" to
        "Shop-Frontend",
    "Situation" to
        "Ausgangslage",
    "SLA (lowest site)" to
        "SLA (schwächster Standort)",
    "SLA at least %@ and less CO₂ than the reference" to
        "SLA mindestens %@ und weniger CO₂ als die Referenz",
    "SLA at least %@ and less CO₂ than the reference operator, who leaves every server on" to
        "SLA mindestens %@ und weniger CO₂ als der Referenzbetreiber, der alle Server laufen lässt",
    "SLA at least 99 % and less CO₂ than the reference" to
        "SLA mindestens 99 % und weniger CO₂ als die Referenz",
    "SLA at least 99 % at each site and less CO₂ than the reference" to
        "SLA mindestens 99 % an jedem Standort und weniger CO₂ als die Referenz",
    "SLA at least 99 %, at most 0.5 ‰ stale answers and less CO₂ than the reference" to
        "SLA mindestens 99 %, höchstens 0,5 ‰ veraltete Antworten und weniger CO₂ als die Referenz",
    "SLA ≥ %@ at each site" to
        "SLA ≥ %@ an jedem Standort",
    "So far %@ t CO₂" to
        "Bisher %@ t CO₂",
    "Spain" to
        "Spanien",
    "Spread again (as found)" to
        "Wieder verteilen (wie vorgefunden)",
    "Spread over all hosts (reference)" to
        "Auf alle Hosts verteilt (Referenz)",
    "Spring" to
        "Frühling",
    "Sprint %lld is running." to
        "Sprint %lld läuft.",
    "Sprint %lld of %lld" to
        "Sprint %1\$lld von %2\$lld",
    "Sprint %lld: plan it now – the game is paused." to
        "Sprint %lld: Jetzt planen – das Spiel ist pausiert.",
    "Sprint planning: the CPU work of a day by part with its complexity, the backlog with what each item saves today, your story points. Below it the situation right now." to
        "Sprint-Planung: die CPU-Arbeit eines Tages nach Teilen mit ihrer Komplexität, der Backlog mit dem, was jeder Punkt heute spart, deine Story Points. Darunter die Lage gerade jetzt.",
    "Sprint planning: where the CPU time per request goes (once measured), the backlog with the team's estimates, your story points. Below it the situation right now." to
        "Sprint-Planung: wohin die CPU-Zeit pro Request geht (sobald gemessen), der Backlog mit den Schätzungen des Teams, deine Story Points. Darunter die Lage gerade jetzt.",
    "Stale answers (‰)" to
        "Veraltete Antworten (‰)",
    "Stale answers: %@" to
        "Veraltete Antworten: %@",
    "Stale answers: %@ (at most %@)" to
        "Veraltete Antworten: %1\$@ (höchstens %2\$@)",
    "start by %@" to
        "spätestens %@",
    "Start now" to
        "Jetzt starten",
    "Start sprint" to
        "Sprint starten",
    "Stores the result instead of recomputing it every 15 minutes; costs storage." to
        "Speichert das Ergebnis, statt es alle 15 Minuten neu zu berechnen; kostet Speicher.",
    "Summer" to
        "Sommer",
    "Switching servers off saves energy, not hardware: the embodied emissions stay the same." to
        "Abschalten spart Strom, aber keine Hardware: Die Herstellungsemissionen bleiben gleich.",
    "Takes effect at once. Switched-off servers stop immediately; switched-on servers boot for 30 minutes." to
        "Wirkt sofort. Abgeschaltete Server stoppen sofort, eingeschaltete starten 30 Minuten lang.",
    "Talks to the live database in Frankfurt all the time: far away it takes %@ times as long." to
        "Spricht ständig mit der Live-Datenbank in Frankfurt: Aus der Ferne dauert er %@-mal so lange.",
    "Target utilization" to
        "Zielauslastung",
    "Target utilization: %@" to
        "Zielauslastung: %@",
    "team: −%@" to
        "Team: −%@",
    "Technical term" to
        "Fachbegriff",
    "Templates, framework and the rest" to
        "Templates, Framework und der Rest",
    "The batch rule: choose one, set it and apply it. Below it the jobs with their deadlines and what the rule has planned for them." to
        "Die Batch-Regel: wählen, einstellen, übernehmen. Darunter die Jobs mit ihren Fristen und was die Regel für sie geplant hat.",
    "The biggest saving today first" to
        "Die größte Einsparung von heute zuerst",
    "The capacity rule: all on, a fixed number, or autoscaling with a target utilization and, if you want, a minimum for a time window. Choose, set, apply. Below it the situation right now." to
        "Die Kapazitätsregel: alle an, eine feste Anzahl oder Autoscaling mit Zielauslastung und auf Wunsch einem Minimum für ein Zeitfenster. Wählen, einstellen, übernehmen. Darunter die Lage gerade jetzt.",
    "The carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done." to
        "Die CO₂-Intensität mit Prognose und darunter eine Zeile pro Job – hell: die Zeit zwischen Ankunft und Frist, umrandet: geplant, gefüllt: läuft oder fertig.",
    "The cleanest energy is the energy not used. 4K needs four times the work of HD – and on a phone screen nobody sees the difference. Demand shaping goes one step further: while the grid is dirty, the service lowers the quality a little, as far as the viewers accept it. Their rating is the limit." to
        "Die sauberste Energie ist die, die nicht verbraucht wird. 4K braucht viermal so viel Arbeit wie HD – und auf einem Smartphone sieht niemand den Unterschied. Demand Shaping geht einen Schritt weiter: Solange der Strom schmutzig ist, senkt der Dienst die Qualität ein wenig, so weit die Zuschauer es akzeptieren. Ihre Bewertung ist die Grenze.",
    "The company runs two data centers, in Warsaw and in Paris, with the same hardware: 24 servers each, bought four years ago. The web shop grows by 15 % a year, at both sites alike. Every year finance approves four new servers for both sites together, and every year the manufacturers bring out a generation that needs less power. From year 3 the old servers are past their six-year life and begin to fail." to
        "Das Unternehmen betreibt zwei Rechenzentren, in Warschau und in Paris, mit derselben Hardware: je 24 Server, vor vier Jahren gekauft. Der Webshop wächst um 15 % pro Jahr, an beiden Standorten gleich. Jedes Jahr gibt die Finanzabteilung vier neue Server für beide Standorte zusammen frei, und jedes Jahr bringen die Hersteller eine Generation, die weniger Strom braucht. Ab dem dritten Jahr sind die alten Server über ihre sechs Jahre hinaus und fallen aus.",
    "The cooling keeps the air at the server inlets at the setpoint. Outside air cools for free when it is at least 6 °C cooler than the setpoint; otherwise the chiller works – the hotter it is outside, the more power it needs. A warmer setpoint saves chiller power, but above 25 °C the server fans speed up." to
        "Die Kühlung hält die Luft am Servereinlass auf dem Sollwert. Außenluft kühlt kostenlos, wenn sie mindestens 6 °C kühler ist als der Sollwert; sonst arbeitet die Kältemaschine – je heißer es draußen ist, desto mehr Strom braucht sie. Ein wärmerer Sollwert spart Strom für die Kältemaschine, aber über 25 °C drehen die Lüfter der Server hoch.",
    "The day is running: the placement is fixed." to
        "Der Tag läuft: Die Verteilung steht fest.",
    "The emissions of making, shipping and disposing of hardware, as opposed to those of the electricity it uses. The SCI books them over the hardware's life; in a clean grid they can be the larger part. Hardware efficiency means using hardware long and well – and replacing it where the energy saved outweighs them." to
        "Die Emissionen von Herstellung, Transport und Entsorgung der Hardware, im Gegensatz zu denen des Stroms, den sie verbraucht. Die SCI verteilt sie über die Lebensdauer der Hardware; in einem sauberen Strommix können sie der größere Teil sein. Hardware-Effizienz heißt: Hardware lange und gut nutzen – und sie dort ersetzen, wo die gesparte Energie sie aufwiegt.",
    "The game pauses at the start of every year. You can pause any time and buy; the servers arrive at the start of the next quarter." to
        "Das Spiel hält zu Beginn jedes Jahres an. Du kannst jederzeit anhalten und kaufen; die Server kommen zu Beginn des nächsten Quartals.",
    "The game starts paused and pauses at the start of every sprint. Clock and speed – pause, 1×, 4×, 16× – and below it your goals." to
        "Das Spiel beginnt pausiert und hält zu Beginn jedes Sprints an. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele.",
    "The game starts paused and pauses at the start of every year. Pause and run – a quarter takes two seconds. Below them your goals: the SLA counts at each site." to
        "Das Spiel startet pausiert und hält zu Beginn jedes Jahres an. Pause und Start – ein Quartal dauert zwei Sekunden. Darunter deine Ziele: Die SLA zählt an jedem Standort.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals." to
        "Das Spiel beginnt pausiert. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. On the right the PUE right now." to
        "Das Spiel startet pausiert. Uhr und Geschwindigkeit – Pause, 1×, 4×, 16× – und darunter deine Ziele. Rechts die PUE gerade jetzt.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals. The game also pauses when a job must start within the hour." to
        "Das Spiel beginnt pausiert. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele. Das Spiel hält auch an, wenn ein Job innerhalb der nächsten Stunde starten muss.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: every job on time, customer data in Germany and less CO₂." to
        "Das Spiel startet pausiert. Uhr und Tempo – Pause, 1×, 4×, 16× – und darunter deine Ziele: jeder Job pünktlich, Kundendaten in Deutschland und weniger CO₂.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the training done by the deadline, and less CO₂." to
        "Das Spiel startet pausiert. Uhr und Geschwindigkeit – Pause, 1×, 4×, 16× – und darunter deine Ziele: das Training bis zur Frist fertig und weniger CO₂.",
    "The game starts paused. Clock and speed – pause, 1×, 4×, 16× – and below it your goals: the viewers' rating and less CO₂." to
        "Das Spiel startet pausiert. Uhr und Geschwindigkeit – Pause, 1×, 4×, 16× – und darunter deine Ziele: die Bewertung der Zuschauer und weniger CO₂.",
    "The GPU servers: dark ones train, the blue bar shows their power." to
        "Die GPU-Server: dunkle trainieren, der blaue Balken zeigt ihre Leistung.",
    "The hosts as columns: the height is the RAM, the VMs are stacked in it. Below each host its CPU over the day – dotted the expected work, the area what has happened, red dashed the line above which requests get slow. Drag a VM onto a host." to
        "Die Hosts als Säulen: Die Höhe ist der RAM, die VMs sind darin gestapelt. Unter jedem Host seine CPU über den Tag – gepunktet die erwartete Arbeit, die Fläche das, was passiert ist, rot gestrichelt die Linie, ab der Requests langsam werden. Zieh eine VM auf einen Host.",
    "The idea" to
        "Die Idee",
    "The jobs are under way – where they run can only be changed before the start (play again)." to
        "Die Jobs laufen schon – wo sie laufen, lässt sich nur vor dem Start ändern (nochmal spielen).",
    "The jobs with their size and deadline. Plan a start time for each job with plus and minus, or start it now; a planned job starts by itself." to
        "Die Jobs mit Größe und Frist. Plane für jeden Job mit Plus und Minus eine Startzeit oder starte ihn sofort; ein geplanter Job startet von selbst.",
    "The model needs a fixed amount of GPU work by the end of day 3. You decide how many GPU servers train together, how much power each may draw, and when they train. A pause is possible at a checkpoint; resuming costs 15 minutes of loading it." to
        "Das Modell braucht eine feste Menge GPU-Arbeit bis Ende von Tag 3. Du entscheidest, wie viele GPU-Server zusammen trainieren, wie viel Leistung jeder ziehen darf und wann sie trainieren. Pausieren geht an einem Checkpoint; das Fortsetzen kostet 15 Minuten zum Laden.",
    "The night is not clean everywhere: in a solar grid the midday hours are the cleanest. Part of the CO₂ is the hardware's manufacturing – it stays the same whenever the jobs run." to
        "Die Nacht ist nicht überall sauber: Im Solarnetz sind die Mittagsstunden am saubersten. Ein Teil des CO₂ stammt aus der Herstellung der Hardware – er bleibt gleich, egal wann die Jobs laufen.",
    "The percentages: how the viewers on that device rate the quality." to
        "Die Prozente: wie die Zuschauer auf diesem Gerät die Qualität bewerten.",
    "The previous team spread the 24 VMs over all ten hosts. Pack them onto fewer hosts – a host without a VM is switched off. Drag a VM onto another host, or click it and then the host. Once the day runs, the placement is fixed." to
        "Das vorige Team hat die 24 VMs auf alle zehn Hosts verteilt. Packe sie auf weniger Hosts – ein Host ohne VM wird abgeschaltet. Zieh eine VM auf einen anderen Host oder klick sie an und dann den Host. Sobald der Tag läuft, steht die Verteilung fest.",
    "The PUE is a ratio, not a goal. Fewer servers saved the most energy – and raised the PUE, because lighting, UPS and pumps draw the same power for fewer servers. A setpoint of 27 °C gives a better PUE than 25 °C, because the faster fans count as IT power – but it uses more energy. All servers on would have had the best PUE of all. Measure CO₂ or energy per request: the PUE only says how well the building serves the IT, not whether the IT does useful work." to
        "Die PUE ist ein Verhältnis, kein Ziel. Weniger Server sparten am meisten Energie – und erhöhten die PUE, weil Licht, USV und Pumpen für weniger Server gleich viel Strom ziehen. Ein Sollwert von 27 °C ergibt eine bessere PUE als 25 °C, weil die schnelleren Lüfter als IT-Leistung zählen – verbraucht aber mehr Energie. Alle Server an hätte die beste PUE von allen gehabt. Miss CO₂ oder Energie pro Request: Die PUE sagt nur, wie gut das Gebäude der IT dient, nicht, ob die IT nützliche Arbeit tut.",
    "The same job emits less CO₂ when the grid is clean. In Spain's solar grid the cleanest hours are around midday, not at night. Plan every job into clean hours before its deadline – and keep in mind that only ten servers are there." to
        "Derselbe Job stößt weniger CO₂ aus, wenn der Strom sauber ist. Im spanischen Solarnetz sind die saubersten Stunden um die Mittagszeit, nicht nachts. Plane jeden Job vor seiner Frist in saubere Stunden – und denk daran, dass nur zehn Server da sind.",
    "The same plus the product videos" to
        "Dasselbe plus die Produktvideos",
    "The same run with exactly the same load and grid mix – only the rules differ:" to
        "Derselbe Ablauf mit genau derselben Last und demselben Strommix – nur die Regeln sind anders:",
    "The same with the nightly warm-up" to
        "Dasselbe mit nächtlichem Vorwärmen",
    "The same without invalidation" to
        "Dasselbe ohne Invalidierung",
    "The same, catalog pages for 1 min instead of invalidation" to
        "Dasselbe, Katalogseiten 1 min statt Invalidierung",
    "The same, eco mode HD above 550 g/kWh" to
        "Dasselbe, Eco-Modus HD über 550 g/kWh",
    "The same, eco mode HD above 650 g/kWh" to
        "Dasselbe, Eco-Modus HD über 650 g/kWh",
    "The same, eco mode SD above 650 g/kWh" to
        "Dasselbe, Eco-Modus SD über 650 g/kWh",
    "The same, greenest hours" to
        "Dasselbe, grünste Stunden",
    "The servers needed for the expected load of the next hour run. A low target leaves headroom for peaks but more servers idle; a high target is efficient but gets slow at peaks." to
        "Es laufen die Server, die für die erwartete Last der nächsten Stunde nötig sind. Ein niedriges Ziel lässt Reserve für Spitzen, aber mehr Server stehen im Leerlauf; ein hohes Ziel ist effizient, wird aber bei Spitzen langsam.",
    "The shop wants a language model for its search, trained by the end of day 3. Eight GPU servers are ready, each up to 10 kW. It is spring in Germany: sunny middays, a calm first day, a windy second day, a calm third. The data science team would start at once, with all servers and without limits." to
        "Der Shop will ein Sprachmodell für seine Suche, trainiert bis Ende von Tag 3. Acht GPU-Server stehen bereit, jeder bis zu 10 kW. Es ist Frühling in Deutschland: sonnige Mittage, ein windstiller erster Tag, ein windiger zweiter, ein windstiller dritter. Das Data-Science-Team würde sofort starten, mit allen Servern und ohne Grenzen.",
    "The team changes nothing (reference)" to
        "Das Team ändert nichts (Referenz)",
    "The VMs" to
        "Die VMs",
    "The web shop again, now in winter and with 30 servers. Autoscaling is on: at 80 % target utilization it keeps just enough servers running. On day 2 marketing sends a newsletter at 19:00 – for three hours twice as many people will shop as usual." to
        "Wieder der Webshop, jetzt im Winter und mit 30 Servern. Autoscaling ist eingeschaltet: Mit 80 % Zielauslastung hält es gerade genug Server am Laufen. An Tag 2 verschickt das Marketing um 19:00 einen Newsletter – drei Stunden lang kaufen doppelt so viele Leute ein wie sonst.",
    "The web shop in a German summer, with 40 servers. A heat wave is coming: 28 °C today, 35 °C tomorrow, 37 °C the day after. Autoscaling runs at 50 % target utilization, the cooling keeps 20 °C at the server inlets – the way the last shift left it. Management watches the PUE: the Energy Efficiency Act asks existing data centers for at most 1.5 from July 2027." to
        "Der Webshop im deutschen Sommer, mit 40 Servern. Eine Hitzewelle kommt: heute 28 °C, morgen 35 °C, übermorgen 37 °C. Das Autoscaling läuft mit 50 % Zielauslastung, die Kühlung hält 20 °C am Lufteinlass der Server – so hat es die letzte Schicht hinterlassen. Die Geschäftsleitung schaut auf die PUE: Das Energieeffizienzgesetz verlangt von bestehenden Rechenzentren ab Juli 2027 höchstens 1,5.",
    "The web shop is growing: every month 15 % more products, customers and orders – after a year almost five times as much data. Each game day stands for a month. The team spends most of its time on features; for performance it has 2 story points per sprint." to
        "Der Webshop wächst: Jeden Monat 15 % mehr Produkte, Kunden und Bestellungen – nach einem Jahr fast fünfmal so viele Daten. Jeder Spieltag steht für einen Monat. Das Team arbeitet vor allem an Features; für Performance hat es 2 Story Points pro Sprint.",
    "The web shop serves five kinds of data: product images, catalog pages, exchange rates, search results and, before a transfer, account balances. Today every request computes or fetches its answer anew. The team has ordered four cache servers with 64 GB each, to be on the safe side; nothing is cached yet. On day 2 at 18:00 a sale starts with new prices in the whole catalog – from then on a page with an old price is a stale answer. Autoscaling keeps the web servers in line with the work." to
        "Der Webshop liefert fünf Arten von Daten: Produktbilder, Katalogseiten, Wechselkurse, Suchergebnisse und vor einer Überweisung den Kontostand. Heute berechnet oder holt jede Anfrage ihre Antwort neu. Das Team hat sicherheitshalber vier Cache-Server mit je 64 GB bestellt; noch liegt nichts im Cache. An Tag 2 um 18:00 beginnt eine Aktion mit neuen Preisen im ganzen Katalog – ab dann ist eine Seite mit altem Preis eine veraltete Antwort. Das Autoscaling hält die Web-Server passend zur Arbeit.",
    "The web shop's 24 virtual machines – front ends, batch workers, reporting, two database replicas and caches – run on ten hosts. The previous team spread them evenly: every host runs, none is busy. A host draws about a quarter of its maximum power even when idle." to
        "Die 24 virtuellen Maschinen des Webshops – Frontends, Batch-Worker, Reporting, zwei Datenbank-Replikate und Caches – laufen auf zehn Hosts. Das vorige Team hat sie gleichmäßig verteilt: Jeder Host läuft, keiner ist ausgelastet. Ein Host zieht auch im Leerlauf etwa ein Viertel seiner Maximalleistung.",
    "The web shop's analytics cluster in Great Britain: twelve servers, nine jobs in three days. It is winter, and the British grid depends on the wind. Today is an ordinary winter day; tomorrow a storm front brings plenty of wind power, the day after the wind drops and gas power plants take over." to
        "Der Analyse-Cluster des Webshops in Großbritannien: zwölf Server, neun Jobs in drei Tagen. Es ist Winter, und das britische Netz hängt am Wind. Heute ist ein gewöhnlicher Wintertag; morgen bringt eine Sturmfront viel Windstrom, übermorgen lässt der Wind nach, und Gaskraftwerke übernehmen.",
    "The web shop's batch cluster in Frankfurt: 14 servers in Germany's autumn grid, about 335 g CO₂ per kWh. The company also rents 8 servers in Oslo – Norway's grid runs on hydropower and emits about 30 g, a tenth. Over three days ten jobs arrive: daily reports, two AI trainings, video encoding, customer analytics, a search index and price calculations." to
        "Der Batch-Cluster des Webshops in Frankfurt: 14 Server im deutschen Herbst-Strommix, rund 335 g CO₂ pro kWh. Die Firma mietet außerdem 8 Server in Oslo – Norwegens Netz läuft mit Wasserkraft und stößt rund 30 g aus, ein Zehntel. In drei Tagen kommen zehn Jobs: Tagesberichte, zwei KI-Trainings, Video-Encoding, eine Kundenanalyse, ein Suchindex und Preisberechnungen.",
    "The web shop's batch cluster in Spain: ten servers for reports, backups, imports and conversions. Over two days seven jobs arrive, each with a deadline. Nobody minds when a job runs – as long as it is finished in time." to
        "Der Batch-Cluster des Webshops in Spanien: zehn Server für Reports, Backups, Importe und Konvertierungen. In zwei Tagen kommen sieben Jobs, jeder mit einer Frist. Wann ein Job läuft, ist egal – solange er rechtzeitig fertig ist.",
    "The work in the data center grows with the bitrate: 4K needs four times as much as HD, SD less than half of HD. Viewers notice the difference on a TV, much less on a phone. Their mean rating must stay at %@ or above." to
        "Die Arbeit im Rechenzentrum wächst mit der Bitrate: 4K braucht viermal so viel wie HD, SD weniger als die Hälfte von HD. Auf einem Fernseher merken die Zuschauer den Unterschied, auf einem Smartphone kaum. Ihre mittlere Bewertung muss bei %@ oder darüber bleiben.",
    "This rule is active." to
        "Diese Regel ist aktiv.",
    "This year's generation needs %@ less power than the first." to
        "Die Generation dieses Jahres braucht %@ weniger Strom als die erste.",
    "This year's generation: the first." to
        "Generation dieses Jahres: die erste.",
    "Timeline shows" to
        "Zeitleiste zeigt",
    "to %@" to
        "bis %@",
    "To scenario %@" to
        "Weiter zu Szenario %@",
    "To the briefing" to
        "Zum Briefing",
    "To the overview" to
        "Zur Übersicht",
    "today: −%@" to
        "heute: −%@",
    "Together they do %@ times the work of one server – each one %@, because they wait for each other." to
        "Zusammen schaffen sie das %1\$@-Fache eines Servers – jeder %2\$@, weil sie aufeinander warten.",
    "Too late for the deadline – start by %@." to
        "Zu spät für die Frist – spätestens %@ starten.",
    "Too little: above 85 % utilization requests get slow." to
        "Zu knapp: Über 85 % Auslastung werden Requests langsam.",
    "too many story points" to
        "zu viele Story Points",
    "Training done" to
        "Training fertig",
    "Training done by the end of day %lld" to
        "Training fertig bis Ende von Tag %lld",
    "Training done by the end of day 3 and less CO₂ than the reference" to
        "Training bis Ende von Tag 3 fertig und weniger CO₂ als die Referenz",
    "Training done: %@" to
        "Training fertig: %@",
    "Training jobs that do not have to finish at a particular hour are moved into the hours with the cleanest electricity, paused at checkpoints and resumed; together with power capping of the GPUs this is one of the most effective levers for the footprint of AI." to
        "Trainingsjobs, die zu keiner bestimmten Stunde fertig sein müssen, werden in die Stunden mit dem saubersten Strom verschoben, an Checkpoints pausiert und fortgesetzt; zusammen mit einem Power Cap für die GPUs ist das einer der wirksamsten Hebel für den Fußabdruck von KI.",
    "Training run" to
        "Trainingslauf",
    "Training run: progress, GPU servers, power cap and when to train – choose, set, apply." to
        "Training: Fortschritt, GPU-Server, Power Cap und wann trainiert wird – wählen, einstellen, übernehmen.",
    "Training – green: progress, blue: GPUs training, light: loading after a pause; dashed: the plan with the rules now" to
        "Training – grün: Fortschritt, blau: GPUs trainieren, hell: Laden nach einer Pause; gestrichelt: der Plan mit den aktuellen Regeln",
    "Trainings, reports and search index to Oslo" to
        "Trainings, Berichte und Suchindex nach Oslo",
    "Trains from now on until the model is done." to
        "Trainiert ab jetzt, bis das Modell fertig ist.",
    "Trains in the hours with the lowest forecast that are needed before the deadline – with a reserve of a tenth." to
        "Trainiert in den Stunden mit der niedrigsten Prognose, die bis zur Frist nötig sind – mit einem Zehntel Reserve.",
    "Transfer: %@ h, %@ kWh in the German grid (about %@ kg CO₂)" to
        "Übertragung: %1\$@ h, %2\$@ kWh im deutschen Strommix (rund %3\$@ kg CO₂)",
    "TVs" to
        "Fernseher",
    "Two and two every year" to
        "Jedes Jahr zwei und zwei",
    "Two sites" to
        "Zwei Standorte",
    "Update the search index incrementally instead of comparing all pairs" to
        "Suchindex inkrementell aktualisieren statt alle Paare zu vergleichen",
    "Update to the current JVM version" to
        "Auf die aktuelle JVM-Version aktualisieren",
    "Upgrade the JSON library" to
        "JSON-Bibliothek aktualisieren",
    "Upgrade the web framework" to
        "Web-Framework aktualisieren",
    "Use a hash map for the product lookup" to
        "Hash-Map für die Produktsuche verwenden",
    "Utilization" to
        "Auslastung",
    "utilization ≥ %@" to
        "Auslastung ≥ %@",
    "Version %@" to
        "Version %@",
    "Video quality" to
        "Videoqualität",
    "Video quality: the highest quality per device with the viewers' rating, the eco mode and its threshold. Below it the situation right now." to
        "Videoqualität: die höchste Qualität je Gerät mit der Bewertung der Zuschauer, der Eco-Modus und seine Schwelle. Darunter die Lage gerade jetzt.",
    "Viewers' rating" to
        "Bewertung der Zuschauer",
    "Viewers' rating %@ (at least %@)" to
        "Bewertung %1\$@ (mindestens %2\$@)",
    "Viewers' rating at least 93 % and less CO₂ than the reference" to
        "Bewertung der Zuschauer mindestens 93 % und weniger CO₂ als die Referenz",
    "Wait for the sun" to
        "Warten auf die Sonne",
    "waiting" to
        "wartet",
    "Warm the cache every night" to
        "Cache jede Nacht vorwärmen",
    "warm-up" to
        "Vorwärmen",
    "Warsaw" to
        "Warschau",
    "Warsaw first, Paris in year 3" to
        "Erst Warschau, Paris im dritten Jahr",
    "Warsaw first, Paris only what it needs, as late as possible" to
        "Erst Warschau, Paris nur so viel wie nötig, so spät wie möglich",
    "Web requests" to
        "Web-Requests",
    "What if …" to
        "Was wäre, wenn …",
    "What saves the most today is not what saves the most in a year. A job with O(n²) is a footnote at the start and a large share of the nights after a year; the rewrite in another language saves only a constant share and blocks the team for months. Ask how the work grows, not only how big it is today. And watch for the rebound effect: capacity that is freed fills up quickly with new features." to
        "Was heute am meisten spart, spart nicht am meisten in einem Jahr. Ein Job mit O(n²) ist anfangs eine Fußnote und nach einem Jahr ein großer Teil der Nächte; das Neuschreiben in einer anderen Sprache spart nur einen konstanten Anteil und blockiert das Team für Monate. Frag, wie die Arbeit wächst, nicht nur, wie groß sie heute ist. Und achte auf den Rebound-Effekt: Frei gewordene Kapazität füllt sich schnell mit neuen Features.",
    "What you did is called %@. %@" to
        "Was du gemacht hast, heißt %1\$@. %2\$@",
    "When the wind drops" to
        "Wenn der Wind nachlässt",
    "Where it goes is not measured yet." to
        "Wohin sie geht, ist noch nicht gemessen.",
    "Whole year" to
        "Jahresmittel",
    "With these settings about %@ hours of training are left; %lld hours remain until the deadline." to
        "Mit diesen Einstellungen bleiben etwa %1\$@ Stunden Training; bis zur Frist sind es noch %2\$lld Stunden.",
    "Without a break" to
        "Ohne Pause",
    "without a break" to
        "ohne Pause",
    "Work per stream" to
        "Arbeit pro Stream",
    "You" to
        "Du",
    "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on." to
        "Du betreibst das Rechenzentrum eines Webshops in Deutschland. Seine 20 Server sind für das Weihnachtsgeschäft ausgelegt. Heute ist ein gewöhnlicher Herbsttag: Abends kaufen viele ein, nachts kaum jemand. Es ist Mitternacht, und die vorige Schicht hat alle Server laufen lassen.",
    "Your batch cluster: grey servers are off, light blue ones are booting, the amber bar shows a server working on a job." to
        "Dein Batch-Cluster: graue Server sind aus, hellblaue starten gerade, der orange Balken zeigt einen Server, der an einem Job arbeitet.",
    "Your racks: grey servers are off, light blue ones are booting, the blue bar shows how busy a server is." to
        "Deine Racks: graue Server sind aus, hellblaue starten gerade, der blaue Balken zeigt, wie ausgelastet ein Server ist.",
    "≤ %lld hosts" to
        "≤ %lld Hosts",
)
