package com.petchat.api.actions;

import com.petchat.api.apiclient.AbstractApiAction;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.queries.chats.GetHistoryQuery;
import com.petchat.api.queries.chats.UpdatePropertiesQuery;

public class Chats extends AbstractApiAction {
    public Chats(PetChatApiClient client) {
        super(client);
    }

    public UpdatePropertiesQuery updateProperties() { return new UpdatePropertiesQuery(getClient()); }
    public GetHistoryQuery getHistory() { return new GetHistoryQuery(getClient()); }
}
