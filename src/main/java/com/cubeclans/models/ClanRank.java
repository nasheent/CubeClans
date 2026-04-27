package com.cubeclans.models;

import java.util.ArrayList;
import java.util.List;

public class ClanRank {
    private final String name;
    private final String displayName;
    private final String prefix;
    private final int ladderPosition;
    private final boolean isDefault;
    private final List<ClanPermission> permissions;
    
    public ClanRank(String name, String displayName, String prefix, int ladderPosition, boolean isDefault, List<String> permissionStrings) {
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.ladderPosition = ladderPosition;
        this.isDefault = isDefault;
        this.permissions = new ArrayList<>();
        
        for (String permString : permissionStrings) {
            ClanPermission perm = ClanPermission.fromString(permString);
            if (perm != null) {
                permissions.add(perm);
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public int getLadderPosition() {
        return ladderPosition;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public List<ClanPermission> getPermissions() {
        return permissions;
    }
    
    public boolean hasPermission(ClanPermission permission) {
        return permissions.contains(ClanPermission.ALL) || permissions.contains(permission);
    }
}
