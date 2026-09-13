package com.petchat.api.actions;

import com.petchat.api.apiclient.AbstractApiAction;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.queries.fcm.FcmRegisterQuery;
import com.petchat.api.queries.fcm.FcmUnregisterQuery;

public class Fcm extends AbstractApiAction {
    public Fcm(PetChatApiClient client) {
        super(client);
    }

    public FcmRegisterQuery register() { return new FcmRegisterQuery(getClient()); }
    public FcmUnregisterQuery unregister() { return new FcmUnregisterQuery(getClient()); }
}
