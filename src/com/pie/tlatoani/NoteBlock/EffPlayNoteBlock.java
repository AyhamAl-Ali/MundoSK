package com.pie.tlatoani.NoteBlock;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.pie.tlatoani.Mundo;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Created by Tlatoani on 4/30/16.
 */
public class EffPlayNoteBlock extends Effect {
    private Expression<Note> noteExpression;
    private Expression<Instrument> instrumentExpression;
    private Expression<Block> blockExpression;
    private Expression<Player> playerExpression = null;

    @Override
    protected void execute(Event event) {
        Block block = blockExpression.getSingle(event);
        NoteBlock noteBlock = (NoteBlock) block.getState();
        if (playerExpression == null) {
            if (instrumentExpression == null) {
                noteBlock.play();
            } else {
                noteBlock.play(instrumentExpression.getSingle(event), (noteExpression == null ? noteBlock.getNote() : noteExpression.getSingle(event)));
            }
        } else {
            Mundo.debug(this, "Block: " + block);
            Mundo.debug(this, "Block.getLocation " + block.getLocation());
            Mundo.debug(this, "Instrument: " + instrumentExpression.getSingle(event));
            Mundo.debug(this, "Note: " + (noteExpression == null ? noteBlock.getNote() : noteExpression.getSingle(event)));
            playerExpression.getSingle(event).playNote(block.getLocation(), instrumentExpression.getSingle(event), (noteExpression == null ? noteBlock.getNote() : noteExpression.getSingle(event)));
        }
    }

    @Override
    public String toString(Event event, boolean b) {
        return "play [[%note% with] %instrument% on] noteblock %block% [for %player%]";
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        noteExpression = (Expression<Note>) expressions[0];
        instrumentExpression = (Expression<Instrument>) expressions[1];
        blockExpression = (Expression<Block>) expressions[2];
        if (i > 0) {
            playerExpression = (Expression<Player>) expressions[3];
        }
        return true;
    }
}
