package com.cubeclans.models;

public enum ClanPermission {
    ALL,            // All permissions (leader only)
    INVITE,         // Invite players to clan
    KICK,           // Kick members from clan
    DISBAND,        // Disband the clan (leader only)
    COLORS,         // Change clan color
    ALLY,           // Manage alliances
    ENEMY,          // Manage enemies
    PVP,            // Toggle friendly fire
    BANK_DEPOSIT,   // Deposit money to clan bank
    BANK_WITHDRAW,  // Withdraw money from clan bank
    BANK_BALANCE,   // View clan bank balance
    BANK_LOGS,      // View bank transaction logs
    CHAT,           // Use clan chat
    PROMOTE,        // Promote clan members
    DEMOTE;         // Demote clan members
    
    public static ClanPermission fromString(String permission) {
        try {
            return ClanPermission.valueOf(permission.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
