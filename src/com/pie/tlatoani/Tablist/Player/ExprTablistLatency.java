package com.pie.tlatoani.Tablist.Player;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.pie.tlatoani.Tablist.Tab;
import com.pie.tlatoani.Tablist.Tablist;
import com.pie.tlatoani.Tablist.Group.TablistProvider;
import com.pie.tlatoani.Util.Static.MathUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Created by Tlatoani on 1/20/18.
 */
public class ExprTablistLatency extends SimpleExpression<Number> {
    private Expression<Player> objectExpression;
    private TablistProvider tablistProvider;

    @Override
    protected Number[] get(Event event) {
        Player object = objectExpression.getSingle(event);
        if (object == null || !object.isOnline()) {
            return new Number[0];
        }
        return tablistProvider
                .view(event)
                .map(tablist -> tablist
                        .getPlayerTablist()
                        .flatMap(playerTablist -> playerTablist.getTab(object))
                        .map(Tab::getLatency)
                        .orElse(null))
                .toArray(Number[]::new);
    }

    @Override
    public boolean isSingle() {
        return tablistProvider.isSingle();
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "tablist latency of " + objectExpression + " for " + tablistProvider;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        objectExpression = (Expression<Player>) expressions[0];
        tablistProvider = TablistProvider.of(expressions, 1);
        return true;
    }

    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        Integer value = mode == Changer.ChangeMode.RESET ? null : ((mode == Changer.ChangeMode.REMOVE ? -1 : 1) * ((Number) delta[0]).intValue());
        Player object = objectExpression.getSingle(event);
        if (object == null || !object.isOnline()) {
            return;
        }
        for (Tablist tablist : tablistProvider.get(event)) {
            tablist
                    .getPlayerTablist()
                    .flatMap(playerTablist -> playerTablist.forceTab(object))
                    .ifPresent(tab -> {
                        if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
                            tab.setLatency(MathUtil.limitToRange(0, tab.getScore().orElse(0) + value, 5));
                        } else {
                            tab.setLatency(MathUtil.limitToRange(0, value, 5));
                        }
                    });
        }
    }

    public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }
}
