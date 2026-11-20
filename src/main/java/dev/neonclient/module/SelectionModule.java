package dev.neonclient.module;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Modulo con una lista di elementi scelti dall'utente in un selettore (blocchi, mob, oggetti). */
public abstract class SelectionModule extends Module {
    public final Set<String> selected = new LinkedHashSet<>();
    protected boolean dirty = true;

    protected SelectionModule(String name, Category category) {
        super(name, category);
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
        dirty = true;
    }

    public void clearSelection() {
        selected.clear();
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
