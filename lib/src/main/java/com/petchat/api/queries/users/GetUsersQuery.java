package com.petchat.api.queries.users;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.users.GetResult;
import com.petchat.api.objects.response.users.SearchResult;
import com.petchat.api.objects.serverdto.UserDto;

import java.util.Collections;
import java.util.List;

public class GetUsersQuery extends AbstractQueryBuilder<GetUsersQuery, GetResult> {

    public GetUsersQuery(PetChatApiClient client) {
        super(client, "users.get", GetResult.class, HttpMethod.GET);
    }

    public GetUsersQuery userId(int value) {
        return queryParam("userId", value);
    }
    public GetUsersQuery shortName(String value) {
        return queryParam("shortName", value);
    }

    @Override protected GetUsersQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return Collections.emptyList(); }
}