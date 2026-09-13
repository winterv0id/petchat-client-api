package com.petchat.api.apiclient;

import com.google.gson.JsonObject;
import com.petchat.api.queries.creditionals.AuthQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AbstractQueryBuilder} - построение запроса: обязательные параметры,
 * сериализация параметров в JSON-тело (POST) или query-строку (GET).
 *
 * <p>{@link PetChatApiClient}/{@code ApacheHttpClient} можно создавать в тестах:
 * конструкторы только настраивают in-memory HTTP-клиент - реального сетевого
 * обращения не происходит, пока не будет выполнен конкретный запрос.
 */
class AbstractQueryBuilderTest {

    private PetChatApiClient client;

    @BeforeEach
    void setup() {
        client = new PetChatApiClient(
                "https://fake.local",
                "test-agent",
                "test-client-key",
                "test-device-id"
        );
    }

    // region essentialKeys
    @Test
    void build_MissingEssentialKey_ThrowsIllegalArgumentException() {
        // essentialKeys() = ["after"], .after(...) не вызван
        var query = client.sync().sync();
        assertThrows(IllegalArgumentException.class, query::build);
    }

    @Test
    void build_AllEssentialKeysProvided_Success() {
        var query = client.sync().sync().after(100L);
        assertDoesNotThrow(query::build);
        assertEquals(100L, query.build().get("after"));
    }

    @Test
    void build_MultipleEssentialKeys_AllMustBeProvided() {
        // AuthQuery требует login и passwd — отсутствие любого должно падать
        var onlyLogin = new AuthQuery(client).login("root");
        assertThrows(IllegalArgumentException.class, onlyLogin::build);

        var allProvided = new AuthQuery(client)
                .login("root")
                .password("superPwd");

        assertDoesNotThrow(allProvided::build);
    }
    // endregion

    // region getQueryString (GET)
    @Test
    void getQueryString_ContainsAllSetParameters() {
        var query = client.sync()
                .sync()
                .after(100L)
                .limit(50);

        String qs = query.getQueryString();

        var pairs = List.of(qs.split("&"));

        assertTrue(pairs.contains("after=100"));
        assertTrue(pairs.contains("limit=50"));
    }

    @Test
    void getQueryString_UrlEncodesSpecialCharacters() {
        var query = client.fcm().register().installationId("");
        query.queryParam("extra", "hello world & char");

        String qs = query.getQueryString();

        assertTrue(
                qs.contains("extra=hello+world+%26+char"),
                "пробел -> '+', '&' -> '%26' (java.net.URLEncoder, UTF-8): " + qs
        );
    }
    //endregion

    // region getJsonObject (POST)
    @Test
    void getJsonObject_SerializesStringAndBooleanParams() {
        var query = new AuthQuery(client)
                .login("root")
                .password("superPwd")
                .refresh(true);

        JsonObject json = query.getJsonObject();

        assertEquals("root", json.get("login").getAsString());
        assertEquals("superPwd", json.get("passwd").getAsString());
        assertTrue(json.get("refresh").getAsBoolean());
    }

    @Test
    void getJsonObject_SerializesIntArrayParam_AsJsonArrayOfNums() {
        var query = client.fcm().register().installationId("");
        query.queryParam("ints", new int[]{1, 2, 3});

        var array = query.getJsonObject().getAsJsonArray("ints");

        assertEquals(3, array.size());
        assertEquals(1, array.get(0).getAsInt());
        assertEquals(3, array.get(2).getAsInt());
    }

    @Test
    void getJsonObject_SerializesCollectionOfStrings_AsJsonArrayOfStrings() {
        var query = client.fcm().register().installationId("");
        query.queryParam("strings", List.of("a", "b"));

        var array = query.getJsonObject().getAsJsonArray("strings");

        assertEquals(2, array.size());
        assertEquals("a", array.get(0).getAsString());
        assertEquals("b", array.get(1).getAsString());
    }
    // endregion
}