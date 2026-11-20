# Neon Client (Fabric, Minecraft 1.21.11, client-side)

Versione: 0.0.3 — premi **Right Shift** in gioco per aprire la GUI.

## Moduli (categoria Render)
- **Freecam** — camera libera client-side, attraversa i muri (impostazioni: velocita', tasto, preferiti)
- **Player ESP** — giocatori con effetto glowing permanente, visibile attraverso i muri; linee tracker opzionali (colore, tasto, preferiti)
- **Block ESP** — blocchi scelti evidenziati attraverso i muri entro la render distance;
  "Select blocks" apre il selettore con ricerca (tutti i blocchi del gioco). Impostazioni: range, linee tracker, colore, tasto, preferiti

## Compilare con GitHub
Ogni push su GitHub avvia il build;
il build parte da solo; il jar e' in Actions -> ultima esecuzione -> Artifacts.

## Versioni
- 0.0.3 — freecam con movimento orizzontale (stile spectator), glowing per Player ESP, linee tracker, GUI ridotta del 25% e spostata a sinistra
- 0.0.2 — Player ESP, Block ESP con selettore blocchi, fix freecam, nuova GUI
- 0.0.1 — prima versione (GUI, Freecam, Customize)
