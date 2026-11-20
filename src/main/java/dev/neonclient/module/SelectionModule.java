package dev.neonclient.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Modulo con una lista di elementi scelti dall'utente in un selettore (blocchi, mob, oggetti). */
public abstract class SelectionModule extends Module {
    public final Set<String> selected = new LinkedHashSet<>();
    /** Colore personalizzato per elemento; gli altri usano il colore di default del modulo. */
    public final Map<String, Integer> colors = new HashMap<>();
    protected boolean dirty = true;
    private Map<String, Catalog.Entry> index;

    protected SelectionModule(String name, Category category) {
        super(name, category);
    }

    /** Elementi scelti all'inizio (e dopo un reset). */
    protected List<String> defaultSelection() {
        return List.of();
    }

    @Override
    public void resetToDefaults() {
        super.resetToDefaults();
        setList(defaultSelection());
        colors.clear();
    }

    /** Elenco completo mostrato nel selettore. */
    public abstract List<Catalog.Entry> catalog();

    /** "blocks", "mobs", "items". */
    public abstract String noun();

    /** Se true, con nessun elemento scelto il modulo vale per tutti. */
    public boolean emptyMeansAll() {
        return false;
    }

    public boolean isSelected(String id) {
        return selected.contains(id);
    }

    public void toggleEntry(String id) {
        if (!selected.remove(id)) selected.add(id);
        else colors.remove(id);
        dirty = true;
    }

    public void clearSelection() {
        selected.clear();
        colors.clear();
        dirty = true;
    }

    public int colorOf(String id) {
        Integer c = colors.get(id);
        return c != null ? c : color;
    }

    public boolean hasCustomColor(String id) {
        return colors.containsKey(id);
    }

    public void setEntryColor(String id, int rgb) {
        Integer old = colors.put(id, rgb & 0xFFFFFF);
        if (old == null || old != (rgb & 0xFFFFFF)) dirty = true;
    }

    public void resetEntryColor(String id) {
        if (colors.remove(id) != null) dirty = true;
    }

    /** Voce del catalogo (nome, icona) per un id. */
    public Catalog.Entry entry(String id) {
        if (index == null) {
            index = new HashMap<>();
            for (Catalog.Entry e : catalog()) index.put(e.id(), e);
        }
        return index.get(id);
    }

    /** Elementi scelti in ordine alfabetico. */
    public List<String> selectedSorted() {
        List<String> out = new ArrayList<>(selected);
        out.sort((a, b) -> {
            String sa = a.startsWith("minecraft:") ? a.substring(10) : a;
            String sb = b.startsWith("minecraft:") ? b.substring(10) : b;
            return sa.compareTo(sb);
        });
        return out;
    }

    @Override
    public Map<String, Integer> getColorMap() {
        return new HashMap<>(colors);
    }

    @Override
    public void setColorMap(Map<String, Integer> map) {
        colors.clear();
        colors.putAll(map);
        dirty = true;
    }

    @Override
    public List<String> getList() {
        return new ArrayList<>(selected);
    }

    @Override
    public void setList(List<String> list) {
        selected.clear();
        selected.addAll(list);
        dirty = true;
    }
}
