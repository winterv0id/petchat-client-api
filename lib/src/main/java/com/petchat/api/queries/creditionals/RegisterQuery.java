package com.petchat.api.queries.creditionals;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.auth.AuthResponse;

import java.util.List;

public class RegisterQuery extends AbstractQueryBuilder<RegisterQuery, AuthResponse> {

    public RegisterQuery(PetChatApiClient client) {
        super(client, "creditionals.register", AuthResponse.class, HttpMethod.POST);
    }

    public RegisterQuery login(String value) {
        return queryParam("login", value);
    }
    public RegisterQuery password(String value) {
        return queryParam("passwd", value);
    }
    public RegisterQuery nickname(String value) {
        return queryParam("nickname", value);
    }

    @Override protected RegisterQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("login", "passwd", "nickname"); }
}
