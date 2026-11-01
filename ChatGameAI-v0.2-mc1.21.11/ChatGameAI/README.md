# ChatGameAI v0.1 (Fabric, Minecraft 1.21.11, Java 21)

Local, API-key-free solver for server chat games. Client-side Fabric mod, built with Mojang mappings.

## Build the jar without installing anything
Push this folder to a GitHub repo, open Actions > build > Run workflow, then download the `chatgameai-jar` artifact.

## Build the jar locally
1. Install JDK 21.
2. Get the Gradle wrapper: generate a 1.21.11 template at https://fabricmc.net/develop and copy its
   `gradlew`, `gradlew.bat` and `gradle/` folder into this project (or run `gradle wrapper` if you have Gradle).
3. Run `./gradlew build` (Windows: `gradlew build`).
4. The mod is `build/libs/chatgameai-0.1.0.jar` (not the `-sources` jar).

## Use on Lunar Client
1. In Lunar, pick 1.21.11 with Fabric, click the gear next to LAUNCH GAME, open the mods folder.
2. Put `chatgameai-0.1.0.jar` there. Lunar may bundle Fabric API; if the game complains it is missing, add Fabric API 0.140.2+1.21.11.

## Layout
- `chatgameai.core.*`: all solving logic, no Minecraft dependency.
- `chatgameai.ChatGameAIClient`: Fabric glue (chat events, delayed send, commands).
- `src/selftest`: plain-Java checks. Compile `src/main/java/chatgameai/core` + this file and run `chatgameai.core.SelfTest`
  with `src/main/resources` on the classpath.

## Behaviour
Each solver scores its answer. Conflicts lower the score, agreement raises it.
- score >= `threshold` (0.85): sent after a random delay, with cooldown
- score >= `suggestFloor` (0.5): shown only to you in grey
- otherwise ignored. Answers starting with `/` are never sent.

Config: `config/chatgameai.properties` (enabled, threshold, suggestFloor, minDelayMs, maxDelayMs, cooldownMs,
listenToPlayerChat, serverAllowlist). Bigger `words.txt` and your own `trivia.tsv` go in `config/chatgameai/`.
Commands: `/chatgameai toggle`, `/chatgameai threshold <0.5-1.0>`.

## v0.2 additions
- Guess-the-number: reads a start prompt like "Guess the number between 1 and 100", then binary-searches using
  "too low / too high / higher / lower" hints. Learns your own typed guesses too. Config: guessMin, guessMax (used when
  the server doesn't state a range).
- Scramble caps: answers in ALL CAPS when the scrambled word is shown in caps or the message mentions capital letters.
  Config `scrambleCase=auto|always|never` (use `always` if your server always wants caps).
