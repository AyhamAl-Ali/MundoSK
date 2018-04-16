package com.pie.tlatoani.Tablist.Player;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.pie.tlatoani.Tablist.Simple.SimpleTablist;
import com.pie.tlatoani.Tablist.Tablist;
import com.pie.tlatoani.Tablist.Group.TablistProvider;
import org.bukkit.event.Event;

/**
 * Created by Tlatoani on 8/11/16.
 */
public class CondPlayersAreVisible extends SimpleExpression<Boolean> {
    private TablistProvider tablistProvider;
    private boolean positive;

    @Override
    protected Boolean[] get(Event event) {
        return new Boolean[]{tablistProvider.check(event, Tablist::arePlayersVisible, positive)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "players are " + (positive ? "visible" : "hidden") + " for " + tablistProvider;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        tablistProvider = TablistProvider.of(expressions, 0);
        positive = parseResult.mark == 0;
        return true;
    }

    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        Boolean visible = positive == (Boolean) delta[0];
        if (visible) {
            for (Tablist tablist : tablistProvider.get(event)) {
                if (!tablist.arePlayersVisible() && !tablist.getPlayerTablist().isPresent()) {
                    tablist.setSupplementaryTablist(SimpleTablist::new);
                }
                tablist.getPlayerTablist().ifPresent(PlayerTablist::showAllPlayers);
            }
        } else {
            for (Tablist tablist : tablistProvider.get(event)) {
                if (tablist.arePlayersVisible() && !tablist.getPlayerTablist().isPresent()) {
                    tablist.setSupplementaryTablist(SimpleTablist::new);
                }
                tablist.getPlayerTablist().ifPresent(PlayerTablist::hideAllPlayers);
            }
        }
    }

    public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Boolean.class);
        }
        return null;
    }
}
