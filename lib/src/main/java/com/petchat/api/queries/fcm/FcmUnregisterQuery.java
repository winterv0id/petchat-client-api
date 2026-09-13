package com.petchat.api.queries.fcm;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;

import java.util.List;

public class FcmUnregisterQuery extends AbstractQueryBuilder<FcmUnregisterQuery, Object> {

    public FcmUnregisterQuery(PetChatApiClient client) {
        super(client, "fcm.unregister", Object.class, HttpMethod.GET);
    }

    public FcmUnregisterQuery installationId(String value) {
        return queryParam("installationId", value);
    }

    @Override protected FcmUnregisterQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("installationId");}
}