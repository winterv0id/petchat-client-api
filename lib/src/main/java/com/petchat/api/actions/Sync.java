package com.petchat.api.actions;

import com.petchat.api.apiclient.AbstractApiAction;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.queries.sync.SyncGetQuery;

public class Sync extends AbstractApiAction {
    public Sync(PetChatApiClient client) {
        super(client);
    }

    public SyncGetQuery sync() { return new SyncGetQuery(getClient()); }
}
