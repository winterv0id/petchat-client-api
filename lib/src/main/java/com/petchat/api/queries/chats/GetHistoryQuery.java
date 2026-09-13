package com.petchat.api.queries.chats;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.chats.GetHistoryResponse;
import java.util.List;

public class GetHistoryQuery extends AbstractQueryBuilder<GetHistoryQuery, GetHistoryResponse> {

    public GetHistoryQuery(PetChatApiClient client) {
        super(client, "chats.getHistory", GetHistoryResponse.class, HttpMethod.GET);
    }

    public GetHistoryQuery chatId(String value) {
        return queryParam("chatId", value);
    }
    public GetHistoryQuery before(int value) { return queryParam("before", value); }
    public GetHistoryQuery limit(Integer value) { return queryParam("limit", value); }

    @Override protected GetHistoryQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("chatId", "before"); }
}