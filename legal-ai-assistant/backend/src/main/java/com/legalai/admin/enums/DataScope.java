package com.legalai.admin.enums;

public enum DataScope {
    SELF(1),
    DEPT(2),
    TEAM(3),
    ALL(4);

    public final int value;

    DataScope(int v) {
        this.value = v;
    }

    public static DataScope fromValue(int value) {
        for (DataScope scope : values()) {
            if (scope.value == value) {
                return scope;
            }
        }
        return ALL;
    }
}
