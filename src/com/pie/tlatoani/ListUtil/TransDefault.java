package com.pie.tlatoani.ListUtil;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.UnparsedLiteral;
import com.pie.tlatoani.Util.Logging;
import org.bukkit.event.Event;

import java.lang.reflect.Array;

/**
 * Created by Tlatoani on 6/15/16.
 */
public class TransDefault implements Transformer {
    Expression expression;
    Boolean isSettable;
    Class returnType;

    @Override
    public Boolean init(Expression expression) {
        Logging.debug(this, "Expression: " + expression);
        if (expression.isSingle()) {
            Skript.error("'" + expression + "' is not a list!");
            return false;
        }
        this.expression = expression;
        if (expression instanceof ExpressionList) {
            this.expression = expression.getConvertedExpression(Object.class);
        }
        returnType = expression.getReturnType();
        isSettable = false;
        Class[] types = expression.acceptChange(Changer.ChangeMode.SET);
        if (types != null) {
            Class arrayclass = Array.newInstance(returnType, 0).getClass();
            for (int i = 0; i < types.length; i++) {
                if (arrayclass.isAssignableFrom(types[i])) {
                    isSettable = true;
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public Class getType() {
        return returnType;
    }

    @Override
    public boolean isSettable() {
        return isSettable;
    }

    @Override
    public Object[] get(Event event) {
        return expression.getArray(event);
    }

    @Override
    public void set(Event event, Object[] value) {
        expression.change(event, value, Changer.ChangeMode.SET);
    }
}
