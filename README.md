# Wave Arena

Ein Wellen-Überlebensmodus für **Minecraft 1.21.11 (Fabric)**.
Du kämpfst in einem selbstgebauten Kolosseum gegen immer härtere Monsterwellen, alle fünf Wellen kommt ein Boss.
Du startest mit schlechter Ausrüstung, verdienst Münzen durch Kills und kaufst beim Händler bessere Waffen, Rüstung und Verbesserungen.

---

## 1. Die Mod bauen (ohne etwas auf dem PC zu installieren)

Die fertige `.jar` baut GitHub für dich:

1. Auf [github.com](https://github.com) einloggen und oben rechts **+ → New repository** wählen. Name zum Beispiel `wavearena`, Sichtbarkeit egal, dann **Create repository**.
2. In dem neuen Repository auf **uploading an existing file** klicken. Den **Inhalt** dieses Ordners (nicht den Ordner selbst) in das Fenster ziehen:
   `.github`, `gradle`, `src`, `build.gradle`, `gradle.properties`, `settings.gradle`, `gradlew`, `gradlew.bat` und die übrigen Dateien.
   Dann unten **Commit changes**.
   - Wichtig: Es muss auch `gradle/wrapper/gradle-wrapper.jar` hochgeladen werden.
   - Falls der Ordner `.github` nicht mitkommt (versteckte Ordner werden manchmal übersprungen): **Add file → Create new file**, als Dateiname `.github/workflows/build.yml` eintippen und den Inhalt der gleichnamigen Datei aus diesem Projekt einfügen.
3. Oben auf den Reiter **Actions** gehen. Der Lauf `build` startet automatisch und dauert ein paar Minuten.
4. Wenn er grün ist: auf den Lauf klicken und unten unter **Artifacts** das Paket **Artifacts** herunterladen. In der ZIP liegt `wavearena-1.0.0.jar`. Die Datei `…-sources.jar` brauchst du nicht.
5. Wenn der Lauf **rot** ist: auf den Lauf klicken, den fehlgeschlagenen Schritt `build` öffnen und die Fehlermeldungen kopieren. Mit diesem Text lässt sich die Mod schnell reparieren.

## 2. Installieren

1. [Fabric Loader](https://fabricmc.net/use/installer/) für **Minecraft 1.21.11** installieren.
2. Die [Fabric API](https://modrinth.com/mod/fabric-api) für 1.21.11 herunterladen.
3. `wavearena-1.0.0.jar` **und** die Fabric API in den Ordner `mods` legen.
4. Das gilt für jeden Mitspieler und für den Server, denn die Mod bringt eigene Gegenstände mit. Mit einem normalen Spiel ohne Mod kann man nicht beitreten.

## 3. Loslegen

1. Eine **neue Welt nur für diesen Modus** erstellen (siehe Hinweise unten), **Cheats erlauben**, am besten Superflach oder ein flaches Gelände.
2. Schwierigkeit auf Leicht oder höher stellen (auf *Friedlich* gibt es keine Monster).
3. Auf freier, flacher Fläche `/wave build` eingeben. Die Arena entsteht in wenigen Sekunden um dich herum (etwa 65 Blöcke Durchmesser, 30 hoch). Alles darin wird weggeräumt.
4. `/wave start` startet die Partie. Du bekommst 30 Münzen, ein Holzschwert und Lederteile.
5. Beim Händler in der Mitte (Rechtsklick oder `/wave shop`) einkaufen, dann geht die erste Welle los.

Mitspieler tippen vorher `/wave join` (oder in der Pause, während eine Partie läuft). Wer `/wave start` eingibt, startet für alle.

## 4. So funktioniert das Spiel

**Ablauf:** Pause (Shop) → Welle → Pause → Welle … Die Wartezeit kannst du mit `/wave ready` verkürzen, sobald alle bereit sind.

**Gegner** werden aus einem Budget zusammengestellt, das mit jeder Welle wächst. Neue Gegnerarten schalten sich nach und nach frei:
Zombies, Skelett-Schützen, Riesenspinnen, Wüstenkrieger, Sprengmeister, Eisensöldner, Axtkämpfer, Armbrustjäger, Hexen, Giftspinnen, Todesritter, Flammengeister, Zerstörer, Beschwörer und Diamantgardisten.
- **Elite-Gegner** leuchten und haben eine Eigenschaft (feurig, vampirisch, explosiv, flink, gepanzert). Sie geben dreifaches Kopfgeld.
- **Wellen-Sonderregeln** tauchen ab Welle 3 zufällig auf: Blutmond, Schwarm, Panzerwelle, Eilmarsch, Goldrausch.
- **Bosse** (jede 5. Welle): Der Knochenfürst, die Hexenkönigin, der Titan, der Todeslord und der Koloss. Danach geht es wieder von vorn, nur stärker. Ihre Angriffe werden vorher mit Partikeln angekündigt, du kannst ausweichen. Für einen Boss gibt es einen Heilkristall.

**Wirtschaft (bewusst nicht zu einfach):**
- Münzen gibt es für Kills (Kopfgeld wächst mit der Welle), für Kill-Serien und als Bonus am Ende jeder Welle.
- Wer stirbt, verliert **30 % seiner Münzen** und schaut bis zum Ende der Welle zu.
- Beute und Erfahrung verschwinden sofort, es gibt keine Möglichkeit, außerhalb des Shops an Ausrüstung zu kommen.
- Es gibt keine natürliche Heilung. Du heilst durch 60 % deiner Leben nach jeder Welle, Heilkristalle und den Seelenernter.
- Rüstung und Waffen aus dem Shop gehen nicht kaputt.

**Shop-Kategorien:** Waffen, Fernkampf, Rüstung, Vorrat und Verbesserungen (mehr Herzen, Schaden, Tempo, Münzbonus, Schärfe, Schutz, Kraftschuss).

**Eigene Waffen:**
| Waffe | Besonderheit |
|---|---|
| Flammenklinge | Setzt Gegner in Brand |
| Frostbrecher | Verlangsamt und schwächt Gegner |
| Seelenernter | Heilt dich bei Treffern |
| Donnerhammer | Rechtsklick: Erdbeben, das alle Gegner im Umkreis trifft und wegschleudert |
| Sturmstab | Rechtsklick: Kettenblitz auf bis zu vier Gegner |
| Heilkristall | Rechtsklick: heilt 4 Herzen |
| Adrenalin | Kurzzeitig schneller und stärker |

## 5. Befehle

| Befehl | Wirkung |
|---|---|
| `/wave help` | Zeigt alle Befehle |
| `/wave build` | Baut die Arena an deiner Position (Cheats / OP nötig) |
| `/wave build keep` | Baut, ohne das Gelände darin wegzuräumen |
| `/wave start` | Startet eine Partie |
| `/wave join` / `/wave leave` | Einsteigen / Aussteigen |
| `/wave shop` | Öffnet den Shop (nur in der Pause) |
| `/wave ready` | Pause überspringen, wenn alle bereit sind |
| `/wave status` | Stand und Bestleistung |
| `/wave stop` | Beendet die Partie und gibt dein Inventar zurück |
| `/wave skip` | Überspringt Pause oder Welle (Cheats / OP) |
| `/wave start <welle>` | Startet bei einer späteren Welle zum Testen (Cheats / OP, kommt nicht in die Bestenliste) |
| `/wave setcenter` | Merkt sich deine Position als Mitte einer schon gebauten Arena (Cheats / OP) |

## 6. Wichtige Hinweise

- **Spiele in einer eigenen Welt.** Beim Start wird dein Inventar zur Seite gelegt und erst am Ende (`/wave stop`, Game Over, Verlassen) zurückgegeben. Wenn das Spiel dazwischen abstürzt, ist dieses Inventar weg. Nimm deshalb keine Welt mit wichtigen Sachen.
- Während einer Partie sind Mobgriefing (Creeper-Explosionen zerstören keine Blöcke) und Blockabbau ausgeschaltet. Nach der Partie wird der alte Wert wiederhergestellt.
- Der Arena-Mittelpunkt und die Bestleistung stehen in `config/wavearena.properties`. Diese Datei gilt für alle Welten, deshalb gehört eine Arena zu einer Welt.
- Spielern im Mehrspieler-Modus werden die Gegner stärker und zahlreicher. Kein Spieler-gegen-Spieler-Schaden.

## 7. Für Bastler

- Werte der Gegner: `game/EnemyType.java`, Bosse: `game/BossType.java`, Wellenaufbau: `game/WaveComposer.java`.
- Shop-Preise: `shop/ShopCatalog.java`, Upgrades: `game/Upgrade.java`.
- Das Aussehen der Arena: `arena/ArenaBuilder.java`.

Lizenz: CC0-1.0 (siehe `LICENSE`).
