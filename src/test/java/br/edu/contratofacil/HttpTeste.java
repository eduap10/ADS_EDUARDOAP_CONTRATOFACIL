package br.edu.contratofacil;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;

final class HttpTeste {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final String base = System.getenv("CF_TEST_URL");
    final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();

    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> form(String path, String formId, Map<String, String> values) throws Exception {
        var response = get(path);
        if (response.statusCode() != 200) throw new IllegalStateException("GET " + path + ": " + response.statusCode());
        var form = Jsoup.parse(response.body()).getElementById(formId);
        if (form == null) throw new IllegalStateException("Formulario ausente: " + formId);
        Map<String, String> body = new LinkedHashMap<>();
        form.select("input[name]").forEach(input -> body.put(input.attr("name"), input.attr("value")));
        body.putAll(values);
        return post(path, body, Map.of());
    }

    HttpResponse<String> post(String path, Map<String, String> body, Map<String, String> headers) throws Exception {
        String encoded = body.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.forEach(request::header);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(encoded)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> login(String login, String password) throws Exception {
        return form("login.xhtml", "formLogin", Map.of("formLogin:login", login, "formLogin:senha", password));
    }

    String sessionId() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID"))
                .map(java.net.HttpCookie::getValue).findFirst().orElse("");
    }

    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
}
