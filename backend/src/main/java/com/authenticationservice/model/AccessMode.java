package com.authenticationservice.model;

/**
 * Access control mode for registration and login.
 * WHITELIST: Only emails in whitelist can register; blacklist is ignored for registration but checked for login.
 * BLACKLIST: All emails can register except those in blacklist; blacklist blocks both registration and login.
 */
public enum AccessMode {
    WHITELIST,
    BLACKLIST
}

