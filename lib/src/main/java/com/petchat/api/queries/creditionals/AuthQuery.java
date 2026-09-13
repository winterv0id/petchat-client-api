package com.petchat.api.queries.creditionals;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.auth.AuthResponse;

import java.util.List;

public class AuthQuery extends AbstractQueryBuilder<AuthQuery, AuthResponse> {

    public AuthQuery(PetChatApiClient client) {
        super(client, "creditionals.auth", AuthResponse.class, HttpMethod.POST);
    }

    public AuthQuery login(String value) {
        return queryParam("login", value);
    }
    public AuthQuery password(String value) {
        return queryParam("passwd", value);
    }
    public AuthQuery refresh(Boolean value) { return queryParam("refresh", value); }

    @Override protected AuthQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("login", "passwd");}
}
