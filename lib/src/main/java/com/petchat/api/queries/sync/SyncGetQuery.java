package com.petchat.api.queries.sync;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.auth.AuthResponse;
import com.petchat.api.objects.response.sync.SyncResult;

import java.util.List;

public class SyncGetQuery extends AbstractQueryBuilder<SyncGetQuery, SyncResult> {

    public SyncGetQuery(PetChatApiClient client) {
        super(client, "sync.sync", SyncResult.class, HttpMethod.GET);
    }

    public SyncGetQuery after(long value) {
        return queryParam("after", value);
    }
    public SyncGetQuery limit(Integer value) {
        return queryParam("limit", value);
    }

    @Override protected SyncGetQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("after");}
}