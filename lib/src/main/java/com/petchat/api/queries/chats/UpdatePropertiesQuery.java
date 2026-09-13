package com.petchat.api.queries.chats;

import com.petchat.api.apiclient.AbstractQueryBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import java.util.List;

public class UpdatePropertiesQuery extends AbstractQueryBuilder<UpdatePropertiesQuery, Object> {

    public UpdatePropertiesQuery(PetChatApiClient client) {
        super(client, "chats.updateProperties", Object.class, HttpMethod.GET);
    }

    public UpdatePropertiesQuery chatId(String value) {
        return queryParam("chatId", value);
    }

    public UpdatePropertiesQuery isNotificationsEnabled(Boolean value) {
        return queryParam("isNotificationsEnabled", value);
    }
    public UpdatePropertiesQuery isArchived(Boolean value) {
        return queryParam("isArchived", value);
    }
    public UpdatePropertiesQuery isPinned(Boolean value) {
        return queryParam("isPinned", value);
    }

    @Override protected UpdatePropertiesQuery getThis() {
        return this;
    }
    @Override protected List<String> essentialKeys() { return List.of("chatId"); }
}