package com.pie.tlatoani.Miscellaneous.Thread;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.Timespan;
import com.pie.tlatoani.Mundo;
import com.pie.tlatoani.Util.CustomScope;
import org.bukkit.event.Event;

/**
 * Created by Tlatoani on 8/25/16.
 */
public class ScopeSync extends CustomScope {
    Expression<Timespan> delay;

    public ScopeSync() {
        canStandFree = true;
    }

    @Override
    public String getString() {
        return delay == null ? "sync" : "in " + delay;
    }

    @Override
    public void setScope() {
        last.setNext(null);
    }

    @Override
    public boolean init() {
        delay = (Expression<Timespan>) exprs[0];
        return true;
    }

    @Override
    public TriggerItem walk(Event event) {
        Runnable runnable = () -> TriggerItem.walk(scope == null ? getNext() : first, event);
        if (delay == null) {
            Mundo.sync(runnable);
        } else {
            Mundo.syncDelay(new Long(delay.getSingle(event).getTicks_i()).intValue(), runnable);
        }
        return null;
    }
}
