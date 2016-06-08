package com.pie.tlatoani.Miscellaneous;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.hanging.HangingBreakEvent;

/**
 * Created by Tlatoani on 5/27/16.
 */
public class EvtUnHang extends SkriptEvent {

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof HangingBreakEvent;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "unhang";
    }
}