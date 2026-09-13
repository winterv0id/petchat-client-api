package com.petchat.api.queries.fcm;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.sync.SyncResult;

import java.util.List;

public class FcmRegisterQuery extends AbstractQueryBuilder<FcmRegisterQuery, Object> {

    public FcmRegisterQuery(PetChatApiClient client) {
        super(client, "fcm.register", Object.class, HttpMethod.GET);
    }

    public FcmRegisterQuery installationId(String value) {
        return queryParam("installationId", value);
    }

    @Override protected FcmRegisterQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("installationId");}
}