# WGF Firework Autofarm

Addon per [Meteor Client](https://meteorclient.com/) che automatizza una firework farm:
compra i materiali dallo shop del server, piazza e rompe la glowstone per ricavarne
la polvere, crafta stelle e razzi, e vende con `/sellall hand`.

| | |
|---|---|
| Minecraft | 1.19.2 |
| Fabric Loader | >= 0.14.0 |
| Meteor Client | 0.5.1 |
| Versione mod | 1.7.0 |

In gioco il modulo compare in Meteor nella categoria **WGF**, con due voci:
`firework-autofarm` (la farm) e `shop-slot-dump` (strumento per mappare lo shop).

---

## 1. Installazione passo passo (Windows)

Tutti i comandi vanno in **PowerShell**: tasto Windows → scrivi `powershell` → Invio.

### 1.1 Controlla di avere 1.19.2 e Fabric

```powershell
Get-ChildItem "$env:APPDATA\.minecraft\versions" -Directory | Where-Object { $_.Name -like "fabric-loader*1.19.2" }
```

Se non esce niente, installa Fabric per **1.19.2** da
[fabricmc.net/use/installer](https://fabricmc.net/use/installer): scheda **Client**,
versione gioco `1.19.2`, spunta **Create profile**, Install.

Java non serve installarlo: per giocare il launcher usa il suo runtime.
Serve solo per compilare (vedi §5).

### 1.2 Usa una cartella di gioco separata

Se la tua `mods` contiene mod di altre versioni, vanno tenute separate: due mod con lo
stesso id, o mod di versioni diverse, impediscono l'avvio.

Nel launcher: **Installazioni** → il profilo `fabric-loader-*-1.19.2` → **⋯** →
**Modifica** → **Altre opzioni** → **Directory di gioco** → `C:\minecraft-1.19.2` → **Salva**.

### 1.3 Scarica i due JAR

```powershell
$m = "C:\minecraft-1.19.2\mods"; New-Item -ItemType Directory -Force -Path $m | Out-Null; curl.exe -L -o "$m\meteor-client-0.5.1.jar" "https://raw.githubusercontent.com/Federicomosc/wgf-firework-autofarm/main/libs/meteor-client-0.5.1.jar"; curl.exe -L -o "$m\wgf-firework-autofarm-1.7.0.jar" "https://raw.githubusercontent.com/Federicomosc/wgf-firework-autofarm/main/build/libs/wgf-firework-autofarm-1.7.0.jar"
```

Fabric API **non** serve: Meteor non lo richiede.

### 1.4 Verifica

```powershell
Get-ChildItem "C:\minecraft-1.19.2\mods" -Filter *.jar | Select-Object Name, Length
```

Devono esserci **due** file, con queste dimensioni esatte:

```
meteor-client-0.5.1.jar          4913002
wgf-firework-autofarm-1.7.0.jar    33670
```

Se una dimensione non corrisponde hai scaricato una pagina di errore invece del JAR:
cancella quel file e rifai il §1.3.

### 1.5 Avvia

Launcher → installazione `fabric-loader-*-1.19.2` → **Gioca**.
Il primo avvio è lento: scarica client e runtime nella cartella nuova.

In gioco premi **Shift destro**: si apre Meteor, e nelle categorie deve comparire **WGF**.

---

## 2. Aggiornare a una versione nuova

Con **Minecraft chiuso**, altrimenti il vecchio JAR è bloccato e la cancellazione
fallisce in silenzio:

```powershell
$m = "C:\minecraft-1.19.2\mods"; Remove-Item "$m\wgf-firework-autofarm-*.jar" -Force -ErrorAction SilentlyContinue; curl.exe -L -o "$m\wgf-firework-autofarm-1.7.0.jar" "https://raw.githubusercontent.com/Federicomosc/wgf-firework-autofarm/main/build/libs/wgf-firework-autofarm-1.7.0.jar"
```

Nel repo resta solo il JAR della versione corrente: i link alle versioni vecchie danno 404.

---

## 3. Configurazione

I valori di fabbrica sono tarati sullo shop del server **WGF**. Se aggiorni da una
versione precedente, Meteor tiene le impostazioni salvate: clicca il pulsante **↻** su
ogni riga per rileggere i default nuovi.

### 3.1 Impostazioni consigliate

**General**

| Impostazione | Valore | Note |
|---|---|---|
| `start-delay` | 240 | attesa prima di partire, in tick (240 = 12 s) |
| `action-delay` | 4 | fra un'azione e l'altra; 1 per andare più veloce |
| `chat-delay` | 20 | dopo un comando in chat; 10 per andare più veloce |
| `gui-wait` | 10 | ritardo minimo dopo un click; 2 per andare più veloce |
| `gui-timeout` | 60 | oltre questo si prosegue lo stesso, avvisando |
| `auto-trova-item` | ON | cerca l'item nella GUI invece di fidarsi del numero di slot |
| `debug-stati` | ON | stampa ogni passaggio di stato: tienilo acceso finché non funziona tutto |
| `auto-sell` | OFF | accendilo solo quando il resto gira |
| `chat-feedback` | ON | senza, non vedi nessun messaggio |

**Crafting**

| Impostazione | Valore | Note |
|---|---|---|
| `crafting-range` | 5 | distanza massima della crafting table |
| `raggio-glowstone` | 1 | **non alzarlo**: oltre 1 blocco i drop restano a terra |

**Safety**

| Impostazione | Valore | Note |
|---|---|---|
| `auto-shutdown` | ON | senza, gli errori non vengono segnalati |
| `detect-teleport` / `detect-velocity` / `detect-chat-flag` | ON | |
| `max-teleport-dist` | 3 | |

**Anti-Detection Vulcan**: tieni `enable-jitter` e `human-like-delays` su **OFF** finché
stai facendo prove — aggiungono varianza casuale che rende i problemi irriproducibili.

### 3.2 Slot dello shop

Default, già corretti per il server WGF:

```
Categorie:  blocchi 19   minerali 13   mobs 21   agricoltura 20   coloranti 23
Pagine:     pagina successiva 14
Item:       glowstone 6   diamond block 8   gunpowder 8   feather 5   sugar cane 18
Coloranti:  cyan 3   purple 12   black 0   gray 4
Acquisto:   item 22   +1 23   +16 24   +32 25   -1 21   -16 20   -32 19   conferma 13
```

Le righe degli item contano poco: con `auto-trova-item` acceso il modulo li cerca da solo
fra le icone della GUI, e usa il numero solo come ripiego.

### 3.3 Su un altro server: ritarare con `shop-slot-dump`

1. Attiva **`shop-slot-dump`** (lascia spento l'autofarm)
2. Apri `/shop` a mano e naviga in ogni categoria e nella schermata di acquisto
3. In chat compare la mappa: `slot 19 = Blocchi`, `slot 14 = Pagina Successiva`, ...
4. Riporta i numeri nel gruppo **Shop Slots**, poi spegni il dump

Per rileggere la chat fuori dal gioco:

```powershell
Select-String -Path "C:\minecraft-1.19.2\logs\latest.log" -Pattern "\[CHAT\]" | ForEach-Object { $_.Line } | Set-Content "$env:USERPROFILE\Desktop\chat.txt"; notepad "$env:USERPROFILE\Desktop\chat.txt"
```

`latest.log` viene sovrascritto a ogni riavvio del gioco: esportalo prima di riavviare.

---

## 4. Come si usa

Prima di attivare servono **tutte** queste condizioni, altrimenti il modulo si ferma
subito:

- **crafting table entro 5 blocchi** da dove stai fermo
- **glowstone nella hotbar**
- **spazio libero tutt'intorno ai piedi** (servono le 8 caselle adiacenti)
- **soldi in conto** per gli acquisti
- server con **`/shop` a GUI** e **`/sellall hand`**

Poi: Shift destro → categoria **WGF** → `firework-autofarm`.
Dopo `start-delay` la sequenza parte e scrive in chat cosa sta facendo.

### Cosa fa, in ordine

1. Compra dallo shop: glowstone, blocco di diamante, polvere da sparo, piume,
   canna da zucchero, quattro coloranti
2. Piazza la glowstone attorno a sé, la rompe e raccoglie la polvere,
   ripetendo finché ne resta
3. Alla crafting table: carta dalla canna da zucchero, diamanti dal blocco,
   stelle, stelle con dissolvenza, razzi
4. `/sellall hand` (solo con `auto-sell` acceso)

---

## 5. Compilare dal sorgente

```bash
JAVA_HOME=/percorso/di/un/jdk-17 ./gradle-8.5/bin/gradle build --no-daemon
```

Il JAR finisce in `build/libs/`. Due vincoli da conoscere:

- **Serve Java 17.** Fabric Loom 1.1.14 non funziona con JDK 18 o successivi.
- **In `build.gradle` il JAR di Meteor deve stare come `modImplementation`.** I JAR di
  release di Meteor sono mappati *intermediary* (`class_2338`, `class_2596`...) mentre il
  sorgente usa i nomi *yarn* (`BlockPos`, `Packet`...): è Loom che li rimappa, e solo
  `modImplementation` innesca la rimappatura. Con `compileOnly` la build fallisce con una
  serie di `cannot access class_XXXX`.

Gradle 8.5 è incluso nel repo, quindi non serve installarlo.

---

## 6. Risoluzione problemi

| Sintomo | Causa |
|---|---|
| Minecraft non parte, il log dice `requires meteor-client` | manca `meteor-client-0.5.1.jar` in `mods` |
| Non parte, il log dice `duplicate mod` | ci sono due JAR del mod nella stessa cartella |
| Non parte, il log dice `requires minecraft 1.19.2` | hai avviato il profilo della versione sbagliata |
| Meteor si apre ma manca la categoria WGF | il mod non è stato caricato: ricontrolla il §1.4 |
| `SHUTDOWN: Crafting Table non trovata` | avvicinati a una crafting table (entro `crafting-range`) |
| `SHUTDOWN: Glowstone non trovata` | mettila nella hotbar |
| `GUI non aggiornata entro N tick` | il server risponde lento: alza `gui-timeout` |
| `STOP: non riesco a impostare la quantità` | i pulsanti +/- della schermata di acquisto non corrispondono: rimappali col dump |
| Rompe i blocchi ma non raccoglie niente | `raggio-glowstone` è maggiore di 1 |
| Si ferma e non dice niente | `chat-feedback` o `auto-shutdown` sono spenti |

Il log completo è in `C:\minecraft-1.19.2\logs\latest.log`.
Con `debug-stati` acceso, l'ultimo `stato: ...` scritto in chat indica il punto esatto
in cui la sequenza si è fermata.

---

## 7. Licenze

Il codice di questo addon è MIT. Il repo però include anche
`libs/meteor-client-0.5.1.jar` e una copia rimappata sotto `.gradle/`: **Meteor Client è
distribuito con licenza GPL-3.0**, incompatibile con la dichiarazione MIT del
`fabric.mod.json`. Se il repo resta pubblico la cosa va sistemata, togliendo i JAR di
Meteor dalla cronologia e lasciando solo il link da cui scaricarli.
