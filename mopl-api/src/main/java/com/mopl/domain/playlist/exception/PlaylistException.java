package com.mopl.domain.playlist.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class PlaylistException extends DomainException {

    public PlaylistException(PlaylistErrorCode errorCode) {
        super(errorCode);
    }
}
