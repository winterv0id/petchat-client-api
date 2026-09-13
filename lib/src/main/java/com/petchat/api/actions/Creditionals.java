package com.petchat.api.actions;

import com.petchat.api.apiclient.AbstractApiAction;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.queries.creditionals.AuthQuery;
import com.petchat.api.queries.creditionals.RegisterQuery;

public class Creditionals extends AbstractApiAction {
    public Creditionals(PetChatApiClient client) {
        super(client);
    }

    public AuthQuery auth() {
        return new AuthQuery(getClient());
    }
    public RegisterQuery register() { return new RegisterQuery(getClient()); }
}
