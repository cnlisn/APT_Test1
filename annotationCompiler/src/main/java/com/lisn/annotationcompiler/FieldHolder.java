package com.lisn.annotationcompiler;

import javax.lang.model.type.TypeMirror;

public class FieldHolder {
    private String name;
    private TypeMirror type;

    public FieldHolder(String name, TypeMirror type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeMirror getType() {
        return type;
    }

    public void setType(TypeMirror type) {
        this.type = type;
    }
}
