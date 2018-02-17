package com.pie.tlatoani.Tablist.Player;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.pie.tlatoani.Tablist.Tab;
import com.pie.tlatoani.Tablist.TablistManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Arrays;

/**
 * Created by Tlatoani on 1/20/18.
 */
public class ExprTablistLatency extends SimpleExpression<Number> {
    private Expression<Player> objectExpression;
    private Expression<Player> playerExpression;

    @Override
    protected Number[] get(Event event) {
        Player object = objectExpression.getSingle(event);
        if (!object.isOnline()) {
            return new Number[0];
        }
        return Arrays
                .stream(playerExpression.getArray(event))
                .filter(Player::isOnline)
                .map(player -> TablistManager
                        .getTablistOfPlayer(player)
                        .getPlayerTablist()
                        .flatMap(playerTablist -> playerTablist.getTab(object))
                        .map(Tab::getLatency)
                        .orElse(null))
                .toArray(Number[]::new);
    }

    @Override
    public boolean isSingle() {
        return playerExpression.isSingle();
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "tablist latency of " + objectExpression + " for " + playerExpression;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        objectExpression = (Expression<Player>) expressions[0];
        playerExpression = (Expression<Player>) expressions[1];
        return true;
    }

    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        Integer value = mode == Changer.ChangeMode.SET ? ((Number) delta[0]).intValue() : 5;
        Player object = objectExpression.getSingle(event);
        if (!object.isOnline()) {
            return;
        }
        for (Player player : playerExpression.getArray(event)) {
            if (!player.isOnline()) {
                continue;
            }
            TablistManager
                    .getTablistOfPlayer(player)
                    .getPlayerTablist()
                    .flatMap(playerTablist -> playerTablist.forceTab(object))
                    .ifPresent(tab -> tab.setLatency(value));
        }
    }

    public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }
}