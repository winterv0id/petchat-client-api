package com.petchat.api.objects.serverdto;

import io.reactivex.rxjava3.annotations.Nullable;

import java.time.Instant;

public class MessageDto {
    public int id;
    public int fromId;
    public int peerId;
    public int index;
    public String chatId;
    public String text;
    public boolean fromOwner;
    public boolean forwarded;
    public boolean readed;
    public boolean edited;
    public Boolean deleted;

    public String date;
    public Instant getDateInstant() {
        return Instant.parse(date);
    }

    public @Nullable String editDate;
    public Instant getEditDateInstant() {
        if (editDate == null)
            return Instant.EPOCH;
        else
            return Instant.parse(editDate);
    }

    public @Nullable String readDate;
    public Instant getReadDateInstant() {
        if (readDate == null)
            return Instant.EPOCH;
        else
            return Instant.parse(readDate);
    }
}
