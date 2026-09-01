# WGF Firework Autofarm

Addon per [Meteor Client](https://meteorclient.com/) che automatizza una firework farm:
acquisto materiali dallo shop, piazzamento/rottura glowstone, crafting e `/sellall hand`.

| | |
|---|---|
| Minecraft | 1.19.2 |
| Fabric Loader | >= 0.14.22 |
| Meteor Client | 0.5.1 |
| Java (build) | 17 |

Il modulo compare in Meteor nella categoria **WGF** con il nome `firework-autofarm`.

## Build

1. Scarica `meteor-client-0.5.1.jar` dalla
   [release 0.5.1 di meteor-archive](https://github.com/ManInMyVan/meteor-archive/releases/tag/0.5.1)
   e mettilo in `libs/` (il JAR non è versionato in questo repo).
2. Lancia lo script per il tuo sistema:
   - Windows: doppio click su `build.bat`
   - macOS / Linux: `./build.sh`

   Entrambi scaricano Gradle 8.5 al primo avvio se non è già presente.
3. Il mod compilato finisce in `build/libs/wgf-firework-autofarm-1.0.0.jar`.

### Java 17 obbligatorio

Fabric Loom 1.1.14 non funziona con JDK più recenti (18+). Se il tuo `java` di sistema
è più nuovo, punta `JAVA_HOME` a un JDK 17 solo per la build:

```bash
JAVA_HOME=/percorso/del/jdk-17 ./gradle-8.5/bin/gradle build --no-daemon
```

### Il JAR di Meteor va dichiarato come `modImplementation`

In `build.gradle` la dipendenza deve restare:

```groovy
modImplementation name: 'meteor-client-0.5.1'
```

I JAR di release di Meteor sono mappati **intermediary** (`class_2338`, `class_2596`, ...),
mentre il sorgente di questo addon usa i nomi **yarn** (`BlockPos`, `Packet`, ...).
`modImplementation` fa rimappare il JAR da Loom prima della compilazione; con `compileOnly`
la rimappatura non avviene e la build fallisce con una serie di
`cannot access class_XXXX` / `incompatible types: class_XXXX cannot be converted to ...`.

## Installazione

Copia il JAR prodotto in:

- Windows: `%appdata%\.minecraft\mods\`
- macOS: `~/Library/Application Support/minecraft/mods/`
- Linux: `~/.minecraft/mods/`

Insieme a Fabric Loader e a `meteor-client-0.5.1.jar`.

## Struttura

```
src/main/java/com/wgf/addon/
├── WgfAddon.java                 # entrypoint Meteor, registra la categoria WGF
└── modules/FireworkAutofarm.java # macchina a stati della farm
src/main/resources/fabric.mod.json
```
