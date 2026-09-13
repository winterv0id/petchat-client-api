package com.petchat.api.queries.users;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.users.SearchResult;

import java.util.List;

public class SearchUsersQuery extends AbstractQueryBuilder<SearchUsersQuery, SearchResult> {

    public SearchUsersQuery(PetChatApiClient client) {
        super(client, "users.search", SearchResult.class, HttpMethod.GET);
    }

    public SearchUsersQuery query(String value) {
        return queryParam("query", value);
    }

    @Override protected SearchUsersQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("query");}
}