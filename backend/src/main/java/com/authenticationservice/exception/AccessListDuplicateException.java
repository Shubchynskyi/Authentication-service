package com.authenticationservice.exception;

import com.authenticationservice.model.AccessListChangeLog;
import lombok.Getter;

/**
 * Thrown when trying to add an email that already exists in whitelist/blacklist.
 */
@Getter
public class AccessListDuplicateException extends RuntimeException {

    private final AccessListChangeLog.AccessListType listType;
    private final String messageKey;
    private final String resolvedMessage;

    public AccessListDuplicateException(AccessListChangeLog.AccessListType listType, String messageKey, String resolvedMessage) {
        super(resolvedMessage);
        this.listType = listType;
        this.messageKey = messageKey;
        this.resolvedMessage = resolvedMessage;
    }
}

