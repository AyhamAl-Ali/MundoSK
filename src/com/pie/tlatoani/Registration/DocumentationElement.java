package com.pie.tlatoani.Registration;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.util.Pair;
import com.pie.tlatoani.Mundo;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Optional;

/**
 * Created by Tlatoani on 8/17/17.
 */
public abstract class DocumentationElement {
    public final String name;
    public final String category;
    public final String[] syntaxes;
    public final String[] description;
    public final String originVersion;
    public final String[] requiredPlugins;

    public enum ElementType {
        EFFECT("Effect"),
        CONDITION("Condition"),
        EXPRESSION("Expression"),
        EVENT("Event"),
        TYPE("Type"),
        SCOPE("Scope");

        public final String toString;

        ElementType(String toString) {
            this.toString = toString;
        }

        public String toString() {
            return toString;
        }
    }

    public abstract ElementType getType();

    public abstract void display(CommandSender sender);

    @Override
    public String toString() {
        return "DocumentationElement(" + category + " " + getType() + ": " + name + ")";
    }

    private DocumentationElement(String name, String category, String[] syntaxes, String description[], String originVersion, String[] requiredPlugins) {
        this.name = name;
        this.category = category;
        this.syntaxes = syntaxes;
        this.description = description;
        this.originVersion = originVersion;
        this.requiredPlugins = requiredPlugins;
    }

    public static class Effect extends DocumentationElement {

        @Override
        public DocumentationElement.ElementType getType() {
            return ElementType.EFFECT;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Effect", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
        }

        public Effect(String name, String category, String[] syntaxes, String description[], String originVersion, String[] requiredPlugins) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
        }
    }

    public static class Condition extends DocumentationElement {

        @Override
        public DocumentationElement.ElementType getType() {
            return ElementType.CONDITION;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Condition", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
        }

        public Condition(String name, String category, String[] syntaxes, String[] description, String originVersion, String[] requiredPlugins) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
        }
    }

    public static class Expression extends DocumentationElement {
        public final ClassInfo type;
        public final Changer[] changers;

        public Expression(String name, String category, String[] syntaxes, String[] description, String originVersion, ClassInfo type, String[] requiredPlugins, Collection<DocumentationBuilder.Changer> changerBuilders) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
            this.type = type;
            this.changers = changerBuilders.stream().map(builder -> builder.build(this)).toArray(Changer[]::new);
        }

        @Override
        public DocumentationElement.ElementType getType() {
            return ElementType.EXPRESSION;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Expression", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            sender.sendMessage(Mundo.formatMundoSKInfo("Type", type.getCodeName()));
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
            if (changers.length > 0) {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Changers");
                for (Changer changer : changers) {
                    changer.display(sender);
                }
            }
        }
    }

    public static class Changer {
        public final Expression parent;
        public final ch.njol.skript.classes.Changer.ChangeMode mode;
        public final Optional<Pair<ClassInfo, Boolean>> type;
        public final String description;
        public final String originVersion;

        public Changer(Expression parent, ch.njol.skript.classes.Changer.ChangeMode mode, Optional<Pair<ClassInfo, Boolean>> type, String description, String originVersion) {
            this.parent = parent;
            this.mode = mode;
            this.type = type;
            this.description = description;
            this.originVersion = originVersion;
        }

        public void display(CommandSender sender) {
            String modeSyntax = mode.name().toLowerCase().replace('_', ' ');
            String typeSyntax = type.map(pair -> " " + pair.getFirst().getCodeName() + (pair.getSecond() ? "" : "s")).orElse("");
            sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + modeSyntax + typeSyntax + (originVersion.equals(parent.originVersion) ? "" : Mundo.TRI_CHAT_COLOR + " Since " + originVersion) + Mundo.ALT_CHAT_COLOR + " " + description);
        }
    }

    public static class Event extends DocumentationElement {
        public final boolean cancellable;
        public final EventValue[] eventValues;

        public Event(String name, String category, String[] syntaxes, String[] description, String originVersion, String[] requiredPlugins, boolean cancellable, Collection<DocumentationBuilder.EventValue> eventValueBuilders) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
            this.cancellable = cancellable;
            this.eventValues = eventValueBuilders.stream().map(builder -> builder.build(this)).toArray(EventValue[]::new);
        }

        @Override
        public DocumentationElement.ElementType getType() {
            return ElementType.EVENT;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Event", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            sender.sendMessage(Mundo.formatMundoSKInfo("Cancellable", cancellable ? "Yes" : "No"));
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
            if (eventValues.length > 0) {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Event Values");
                for (EventValue eventValue : eventValues) {
                    eventValue.display(sender);
                }
            }
        }
    }

    public static class EventValue {
        public final Event parent;
        public final ClassInfo type;
        public final String description;
        public final String originVersion;

        public EventValue(Event parent, ClassInfo type, String description, String originVersion) {
            this.parent = parent;
            this.type = type;
            this.description = description;
            this.originVersion = originVersion;
        }

        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "event-" + type.getCodeName() + (originVersion.equals(parent.originVersion) ? "" : Mundo.TRI_CHAT_COLOR + " Since " + originVersion) + Mundo.ALT_CHAT_COLOR + " " + description);
        }
    }

    public static class Type extends DocumentationElement {
        public final String[] usages;

        public Type(String name, String category, String[] syntaxes, String[] usages, String[] description, String originVersion, String[] requiredPlugins) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
            this.usages = usages;
        }

        @Override
        public ElementType getType() {
            return ElementType.TYPE;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Type", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            sender.sendMessage(Mundo.formatMundoSKInfo("Usages", syntaxes.length == 0 ? "Cannot be written in scripts" : String.join(", ", usages)));
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
        }
    }

    public static class Scope extends DocumentationElement {

        @Override
        public DocumentationElement.ElementType getType() {
            return ElementType.EFFECT;
        }

        @Override
        public void display(CommandSender sender) {
            sender.sendMessage(Mundo.formatMundoSKInfo(category + " Scope", name));
            sender.sendMessage(Mundo.formatMundoSKInfo("Since", "MundoSK " + originVersion));
            if (requiredPlugins.length > 0) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Required Plugins", String.join(" ", requiredPlugins)));
            }
            if (syntaxes.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Syntax", syntaxes[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Syntaxes");
                for (String syntax : syntaxes) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + syntax);
                }
            }
            if (description.length == 1) {
                sender.sendMessage(Mundo.formatMundoSKInfo("Description", description[0]));
            } else {
                sender.sendMessage(Mundo.PRIMARY_CHAT_COLOR + "Description");
                for (String descLine : description) {
                    sender.sendMessage(Mundo.ALT_CHAT_COLOR + descLine);
                }
            }
        }

        public Scope(String name, String category, String[] syntaxes, String description[], String originVersion, String[] requiredPlugins) {
            super(name, category, syntaxes, description, originVersion, requiredPlugins);
        }
    }
}
