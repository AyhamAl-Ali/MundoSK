package com.pie.tlatoani.Registration;

import ch.njol.skript.classes.ClassInfo;

/**
 * Created by Tlatoani on 9/9/17.
 */
public class MundoClassInfo<T> extends ClassInfo<T> implements DocumentationBuilder<DocumentationElement.Type, MundoClassInfo<T>> {
    protected String name = null;
    protected String category = null;
    protected String[] syntaxes = null;
    protected String description = null;
    protected String originVersion = null;
    protected String[] requiredPlugins = null;

    public MundoClassInfo(Class<T> c, String[] names, String category) {
        super(c, names[0]);
        this.syntaxes = names;
        this.category = category;
        user(names);
        name(names[0]);
    }

    @Override
    public DocumentationElement.Type build() {
        return new DocumentationElement.Type(name, category, syntaxes, new String[0], description, originVersion, requiredPlugins);
    }

    public MundoClassInfo<T> document(String name, String description, String originVersion) {
        Documentation.addBuilder(this);
        this.name = name;
        this.description = description;
        this.originVersion = originVersion;
        return this;
    }

    public MundoClassInfo<T> requiredPlugins(String... plugins) {
        requiredPlugins = plugins;
        return this;
    }
}