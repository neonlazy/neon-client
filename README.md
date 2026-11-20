# Neon Client (Fabric, Minecraft 1.21.11, client-side)

Versione: 0.0.5 — premi **Right Shift** in gioco per aprire la GUI.

## Moduli (categoria Render, in ordine alfabetico)
- **Block ESP** — blocchi scelti con effetto glowing, visibili attraverso i muri entro la render distance
  (massimo 320, i piu' vicini). I blocchi senza modello (barrier, acqua, lava, light...) hanno un riquadro.
  Impostazioni: selettore blocchi con ricerca, range, linee tracker, colore, tasto, preferiti
- **Entity ESP** — mob con effetto glowing; selettore entita' (nessuna scelta = tutti i mob), linee tracker, colore, tasto, preferiti
- **Freecam** — camera libera client-side (WASD in orizzontale, Space/Shift su/giu');
  il corpo fermo puo' rompere i blocchi entro la sua portata. Impostazioni: velocita', tasto, preferiti
- **Item ESP** — oggetti droppati con effetto glowing; selettore oggetti (nessuna scelta = tutti), linee tracker, colore, tasto, preferiti
- **Player ESP** — giocatori con effetto glowing; linee tracker, colore, tasto, preferiti

## GUI
- Schede in alto: Modules, Config, Customize
- Colonna **Search** (dopo Favorites) per cercare i moduli per nome
- Impostazioni degli ESP con selettore: colore di default + lista degli elementi scelti, tasto destro su un elemento per cambiarne il colore
- **Customize**: carattere (Minecraft / Default / Vanilla+), stile e colori dello sfondo, stile (Solid / Gradient / Glass) e colori dei pannelli dei moduli

## Caratteri inclusi
- Default: Inter (SIL Open Font License 1.1)
- Vanilla+: Pixelify Sans (SIL Open Font License 1.1) — carattere pixel morbido simile allo stile Vanilla+, non il font originale del pack
Licenze in `src/main/resources/assets/neonclient/font/`.

## Compilare
Ogni push su GitHub avvia il build; il jar e' in Actions -> ultima esecuzione -> Artifacts.

## Versioni
- 0.0.5 — colori per singolo blocco/mob/oggetto negli ESP, scelta del carattere, schede Macros e Profiles rimosse, Modules prima scheda, Search dopo Favorites
- 0.0.4 — glowing per Block ESP, Entity ESP, Item ESP, colonna Search, personalizzazione pannelli, rottura blocchi in freecam, GUI ridotta di un altro 25%, tutto in ordine alfabetico
- 0.0.3 — freecam con movimento orizzontale (stile spectator), glowing per Player ESP, linee tracker, GUI ridotta del 25% e spostata a sinistra
- 0.0.2 — Player ESP, Block ESP con selettore blocchi, fix freecam, nuova GUI
- 0.0.1 — prima versione (GUI, Freecam, Customize)
