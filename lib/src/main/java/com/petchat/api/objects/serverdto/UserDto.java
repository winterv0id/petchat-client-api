package com.petchat.api.objects.serverdto;

import io.reactivex.rxjava3.annotations.Nullable;

import java.time.Instant;

public class UserDto {
    public int id;
    public @Nullable String shortName = null;
    public String nickname;
    public @Nullable String status = null;
    public @Nullable String imageUrl = null;
    public boolean isOnline;

    public String lastSeen;
    public Instant getLastSeenInstant() {
        return Instant.parse(lastSeen);
    }
}
