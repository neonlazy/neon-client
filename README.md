# Neon Client (Fabric, Minecraft 1.21.11, client-side)

Versione: 0.0.7 — premi **Right Shift** in gioco per aprire la GUI.

## Moduli (categoria Render, in ordine alfabetico)
- **Block ESP** — blocchi scelti con effetto glowing, visibili attraverso i muri entro la render distance
  (massimo 320, i piu' vicini). I blocchi senza modello (barrier, acqua, lava, light...) hanno un riquadro.
  Impostazioni: selettore blocchi con ricerca, Storage ESP (tutti i contenitori: chest, copper chest, barrel,
  hopper, shulker box...), range, linee tracker, colori per blocco, tasto, preferiti
- **Entity ESP** — mob con effetto glowing; selettore entita' (nessuna scelta = tutti i mob), linee tracker, colore, tasto, preferiti
- **Freecam** — camera libera client-side (WASD in orizzontale, Space/Shift su/giu');
  il tasto sinistro rompe il blocco che sta guardando il corpo fermo. Impostazioni: velocita', tasto, preferiti
- **Item ESP** — oggetti droppati con effetto glowing; selettore oggetti (nessuna scelta = tutti), linee tracker, colore, tasto, preferiti
- **Player ESP** — giocatori con effetto glowing; linee tracker, colore, tasto, preferiti
- **Xray** — mostra solo i minerali con luce piena (nessuna impostazione oltre a tasto e preferiti)

## GUI
- Schede in alto: Modules, Customize
- "Reset modules" in basso a sinistra nella scheda Modules (con click di conferma)
- Click sul titolo di una categoria per nasconderne i moduli; con **Free Move** attivo i titoli si trascinano e i moduli seguono
- Colonna **Search** (dopo Favorites) per cercare i moduli per nome
- Impostazioni degli ESP con selettore: colore di default + lista degli elementi scelti, tasto destro su un elemento per cambiarne il colore
- **Customize**: carattere (Minecraft / Default / Alternative), dimensione della GUI (25-300%), Free Move, stile e colori dello sfondo, stile (Solid / Gradient / Glass) e colori dei pannelli dei moduli; "Reset appearance" in basso a sinistra (con click di conferma)

## Caratteri inclusi
- Default: Inter (SIL Open Font License 1.1)
- Alternative: Pixelify Sans (SIL Open Font License 1.1) — carattere pixel morbido simile allo stile Vanilla+, non il font originale del pack
Licenze in `src/main/resources/assets/neonclient/font/`.

## Compilare
Ogni push su GitHub avvia il build; il jar e' in Actions -> ultima esecuzione -> Artifacts.

## Versioni
- 0.0.7 — colori giallo e arancione, colori rapidi in ordine di sfumatura, Xray senza opzioni, Reset modules nella scheda Modules, la freecam rompe dove guarda il corpo
- 0.0.6 — Xray, Storage ESP, dimensione GUI 25-300%, Free Move e categorie richiudibili, reset dei moduli, carattere "Alternative", scheda Config rimossa
- 0.0.5 — colori per singolo blocco/mob/oggetto negli ESP, scelta del carattere, schede Macros e Profiles rimosse, Modules prima scheda, Search dopo Favorites, reset dell'aspetto in Customize
- 0.0.4 — glowing per Block ESP, Entity ESP, Item ESP, colonna Search, personalizzazione pannelli, rottura blocchi in freecam, GUI ridotta di un altro 25%, tutto in ordine alfabetico
- 0.0.3 — freecam con movimento orizzontale (stile spectator), glowing per Player ESP, linee tracker, GUI ridotta del 25% e spostata a sinistra
- 0.0.2 — Player ESP, Block ESP con selettore blocchi, fix freecam, nuova GUI
- 0.0.1 — prima versione (GUI, Freecam, Customize)
