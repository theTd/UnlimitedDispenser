package org.totemcraft.unlimiteddispenser;

public enum Permissions {
    ACCESS("unlimiteddispenser.access");

    private String permission;

    Permissions(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        return getPermission();
    }
}
