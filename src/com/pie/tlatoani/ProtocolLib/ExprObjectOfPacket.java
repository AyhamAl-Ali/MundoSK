package com.pie.tlatoani.ProtocolLib;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.pie.tlatoani.Mundo;
import org.bukkit.event.Event;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Tlatoani on 5/2/16.
 */
public class ExprObjectOfPacket extends SimpleExpression<Object> {
    private Method getObjects = null;
    private Expression<Number> index;
    private Expression<PacketContainer> packetContainerExpression;
    private static Field structureModifier;
    private Boolean isSingle = true;
    private Class aClass;

    static {
        try {
            structureModifier = PacketContainer.class.getDeclaredField("structureModifier");
            structureModifier.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Object[] get(Event event) {
        StructureModifier structureModifier = null;
        if (getObjects != null) {
            try {
                structureModifier = (StructureModifier) getObjects.invoke(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                Mundo.debug(this, e);
            } catch (InvocationTargetException e) {
                Mundo.debug(this, e);
            }
        } else {
            try {
                structureModifier = (StructureModifier) ExprObjectOfPacket.structureModifier.get(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (isSingle) {
            return new Object[] {structureModifier.readSafely(index.getSingle(event).intValue())};
        } else {
            return (Object[]) structureModifier.readSafely(index.getSingle(event).intValue());
        }
    }

    @Override
    public Iterator<Object> iterator(Event event) {
        if (isSingle) {
            throw new UnsupportedOperationException("This is not an array!");
        }
        StructureModifier structureModifier = null;
        if (getObjects != null) {
            try {
                structureModifier = (StructureModifier) getObjects.invoke(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                Mundo.debug(this, e);
            } catch (InvocationTargetException e) {
                Mundo.debug(this, e);
            }
        } else {
            try {
                structureModifier = (StructureModifier) ExprObjectOfPacket.structureModifier.get(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return Arrays.asList((Object[]) structureModifier.readSafely(index.getSingle(event).intValue())).iterator();
    }

    @Override
    public boolean isSingle() {
        return isSingle;
    }

    @Override
    public Class<?> getReturnType() {
        return aClass;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "%type% packetinfo %number% of %packet%";
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        if (i == 0 || i == 1) {
            Literal<ClassInfo<?>> literal = (Literal<ClassInfo<?>>) expressions[0];
            index = (Expression<Number>) expressions[1];
            packetContainerExpression = (Expression<PacketContainer>) expressions[2];
            aClass = literal.getSingle().getC();
            String classname = aClass.getSimpleName();
            if (aClass == Object.class) {
                return true;
            }
            Mundo.debug(this, "Class simple name: " + classname);
            String pluralclassname;
            if (i == 1) {
                pluralclassname = classname + "Arrays";
                isSingle = false;
            } else if (classname.substring(classname.length() - 1).equals("y")) {
                pluralclassname = classname.substring(0, classname.length() - 1) + "ies";
            } else {
                pluralclassname = classname + "s";
            }
            Mundo.debug(this, "Class plural name: " + pluralclassname);
            try {
                Method method = PacketContainer.class.getMethod("get" + pluralclassname);
                Mundo.debug(this, "Method: " + method.toString());
                getObjects = method;
            } catch (NoSuchMethodException e) {
                Mundo.debug(this, e);
                Skript.error("The type " + literal + " is not applicable for the '%type% %number% of %packet%' expression.");
                return false;
            }
            return true;
        } else {
            String classname;
            if (expressions[0] instanceof Literal<?>) {
                classname = ((Literal<String>) expressions[0]).getSingle();
            } else if (expressions[0] instanceof VariableString) {
                String fullstring = ((VariableString) expressions[0]).toString();
                classname = fullstring.substring(1, fullstring.length() - 1);
            } else {
                Skript.error("The string '" + expressions[0] + "' is not a literal string! Only literal strings can be used in the pjson expression!");
                return false;
            }
            index = (Expression<Number>) expressions[1];
            packetContainerExpression = (Expression<PacketContainer>) expressions[2];
            Mundo.debug(this, "Method name without 'get': " + classname);
            try {
                Method method = PacketContainer.class.getMethod("get" + classname);
                Mundo.debug(this, "Method: " + method.toString());
                getObjects = method;
                aClass = method.getReturnType();
            } catch (NoSuchMethodException e) {
                Mundo.debug(this, e);
                Skript.error("There is no packet info method called 'get" + classname + "'!");
                return false;
            }
            return true;
        }
    }

    public void change(Event event, Object[] delta, Changer.ChangeMode mode){
        StructureModifier structureModifier = null;
        if (getObjects != null) {
            try {
                structureModifier = (StructureModifier) getObjects.invoke(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                Mundo.debug(this, e);
            } catch (InvocationTargetException e) {
                Mundo.debug(this, e);
            }
        } else {
            try {
                structureModifier = (StructureModifier) ExprObjectOfPacket.structureModifier.get(packetContainerExpression.getSingle(event));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        structureModifier.writeSafely(index.getSingle(event).intValue(), delta[0]);
    }

    public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            if (isSingle) {
                return CollectionUtils.array(aClass);
            } else {
                return CollectionUtils.array(Object[].class);
            }
        }
        return null;
    }
}
