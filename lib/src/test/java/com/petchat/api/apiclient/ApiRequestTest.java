package com.petchat.api.apiclient;

import com.google.gson.JsonObject;
import com.petchat.api.exceptions.ApiClientException;
import com.petchat.api.exceptions.ApiServerException;
import com.petchat.api.exceptions.NotAuthorizedException;
import com.petchat.api.httpclient.HttpClient;
import com.petchat.api.httpclient.ProgressListener;
import com.petchat.api.objects.response.auth.AuthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ApiRequest#executeAsClientResponse()} проверяет статус ответа сервера
 * и выбрасывает конкретное исключение, если сервер вернул ошибку.
 * Используется {@link FakeHttpClient} (реализация интерфейса {@link HttpClient}, без сети).
 *
 * <p>Тестируется граница ошибки: статусы {@code < 400} - успех,
 * {@code >= 400} - исключение (401 отдельно как {@link NotAuthorizedException},
 * 4xx - {@link ApiClientException}, 5xx - {@link ApiServerException}).
 */
class ApiRequestTest {
    @Test
    void statusBelow400_IsCompletedSuccessfully_AndParsedAsResponseType() throws Exception {
        var httpClient200 = new FakeHttpClient(200, "{\"accessToken\":\"token12345_code200\"}");
        var httpClient399 = new FakeHttpClient(302, "{\"accessToken\":\"token12345_code399\"}");

        var request200 = new TestableApiRequest(httpClient200, ApiRequest.HttpMethod.GET);
        var request399 = new TestableApiRequest(httpClient399, ApiRequest.HttpMethod.GET);

        AuthResponse result200 = request200.execute().get(2, TimeUnit.SECONDS),
                result399 = request399.execute().get(2, TimeUnit.SECONDS);

        assertEquals("token12345_code200", result200.accessToken);
        assertEquals("token12345_code399", result399.accessToken);
    }

    @ParameterizedTest(name = "Status {0} should throw {1}")
    @MethodSource("provideStatusCodesAndExceptions")
    void statusCodes_ThrowExpectedExceptions(int statusCode, Class<? extends Throwable> expectedExceptionClass) {
        var httpClient = new FakeHttpClient(statusCode, getMockResponseBody(statusCode));
        var request = new TestableApiRequest(httpClient, ApiRequest.HttpMethod.GET);

        var thrown = assertThrows(
                ExecutionException.class,
                () -> request.executeAsClientResponse().get(2, TimeUnit.SECONDS)
        );

        Throwable rootException = unwrapException(thrown);
        assertInstanceOf(expectedExceptionClass, rootException);

        if (rootException instanceof ApiClientException clientEx) {
            assertEquals(statusCode, clientEx.statusCode);
        } else if (rootException instanceof ApiServerException serverEx) {
            assertEquals(statusCode, serverEx.statusCode);
        }
    }

    private static Stream<Arguments> provideStatusCodesAndExceptions() {
        return Stream.of(
                // ApiClientException
                Arguments.of(400, ApiClientException.class),
                Arguments.of(404, ApiClientException.class),
                Arguments.of(499, ApiClientException.class),

                // NotAuthorizedException
                Arguments.of(401, NotAuthorizedException.class),

                // ApiServerException
                Arguments.of(500, ApiServerException.class),
                Arguments.of(502, ApiServerException.class)
        );
    }

    private String getMockResponseBody(int statusCode) {
        return statusCode == 401 ? "unauthorized" :
                String.format("{\"error\":\"mock error for %d\"}", statusCode);
    }

    @Test
    void postMethod_UsesJsonBody_NotQueryString() throws Exception {
        var httpClient = new FakeHttpClient(200, "{\"content\":\"no content\"}");
        var request = new TestableApiRequest(httpClient, ApiRequest.HttpMethod.POST);

        request.execute().get(2, TimeUnit.SECONDS);

        assertTrue(
                httpClient.postCalled,
                "для POST должен вызываться client.post(...), а не client.get(...)"
        );
        assertFalse(httpClient.getCalled);
    }

    // region helpers
    private static Throwable unwrapException(ExecutionException ex) {
        Throwable cause = ex.getCause();
        return (cause instanceof CompletionException) ? cause.getCause() : cause;
    }

    // минимальный ApiRequest для теста
    private static class TestableApiRequest extends ApiRequest<AuthResponse> {
        TestableApiRequest(HttpClient client, HttpMethod method) {
            super("http://super.api/test.endpoint", client, AuthResponse.class, method);
        }

        @Override
        protected String getQueryString() {
            return "";
        }

        @Override
        protected JsonObject getJsonObject() {
            return new JsonObject();
        }
    }

    // заглушка HttpClient — без сети, отдаёт заранее заданный статус/тело
    private static class FakeHttpClient implements HttpClient {
        private final int statusCode;
        private final String content;
        boolean getCalled = false;
        boolean postCalled = false;

        FakeHttpClient(int statusCode, String content) {
            this.statusCode = statusCode;
            this.content = content;
        }

        private Response bakedResponse() {
            return new Response(statusCode, content, Map.of());
        }

        @Override public Response get(String url) { getCalled = true; return bakedResponse(); }
        @Override public Response get(String url, String parameters) { getCalled = true; return bakedResponse(); }
        @Override public Response get(String url, String parameters, String contentType) { getCalled = true; return bakedResponse(); }

        @Override public Response post(String url, JsonObject jsonObject) { postCalled = true; return bakedResponse(); }
        @Override public Response post(String url, String body) { postCalled = true; return bakedResponse(); }
        @Override public Response post(String url, Map<String, File> files) { postCalled = true; return bakedResponse(); }
        @Override public Response post(String url, String body, String contentType) { postCalled = true; return bakedResponse(); }

        @Override public Response delete(String url) { return bakedResponse(); }
        @Override public Response delete(String url, String body) { return bakedResponse(); }
        @Override public Response delete(String url, String body, String contentType) { return bakedResponse(); }

        @Override public void downloadFile(String url, File file) { throw new UnsupportedOperationException(); }
        @Override public void downloadFile(String url, File outputFile, ProgressListener listener) { throw new UnsupportedOperationException(); }
    }
    // endregion
}
