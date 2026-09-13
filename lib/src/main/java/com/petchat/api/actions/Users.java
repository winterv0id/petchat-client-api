package com.petchat.api.actions;

import com.petchat.api.apiclient.AbstractApiAction;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.queries.users.GetUsersQuery;
import com.petchat.api.queries.users.SearchUsersQuery;

public class Users extends AbstractApiAction {
    public Users(PetChatApiClient client) {
        super(client);
    }

    public SearchUsersQuery search() { return new SearchUsersQuery(getClient()); }
    public GetUsersQuery get() { return new GetUsersQuery(getClient()); }
}
