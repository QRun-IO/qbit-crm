/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.crm.api;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qbits.crm.webhook.CrmHookRouteProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real HTTP proves mail-scanner GET requests never change subscription preferences.
 *******************************************************************************/
class CrmHookRoutesTest extends BaseTest
{
   /*******************************************************************************
    ** Confirm and unsubscribe require post and reject bad tokens.
    *******************************************************************************/
   @Test
   void confirmAndUnsubscribeRequirePostAndRejectBadTokens() throws Exception
   {
      CrmQBitConfig config = CrmApi.config().withTokenSecret("route-test-secret").withPublicBaseUrl("https://example.test");
      Integer contactId = CrmStore.asUser().insert("crm_contact", new QRecord().withValue("firstName", "Private").withValue("lastName", "Person")
         .withValue("email", "private@example.test").withValue("ownerUserId", TEST_USER_ID)).getValueInteger("id");
      Integer listId = CrmStore.asUser().insert("crm_email_list", new QRecord().withValue("listKey", "news").withValue("slug", "news")
         .withValue("name", "News").withValue("sortOrder", 1)).getValueInteger("id");
      Integer memberId = CrmStore.asUser().insert("crm_email_list_member", new QRecord().withValue("contactId", contactId).withValue("emailListId", listId)
         .withValue("email", "private@example.test").withValue("status", 5).withValue("consentText", "Send me news")).getValueInteger("id");
      CrmTokens tokens = new CrmTokens(config);
      String confirm = tokens.issue("confirm", memberId, listId);
      CrmStore.asUser().update("crm_email_list_member", new QRecord().withValue("id", memberId).withValue("confirmationTokenHash", CrmTokens.hash(confirm)));
      CrmHookRouteProvider routes = new CrmHookRouteProvider();
      routes.setQInstance(QContext.getQInstance());
      CrmHookRouteProvider.register(QContext.getQInstance());
      CrmHookRouteProvider.register(QContext.getQInstance());
      assertThat(QJavalinMetaData.ofOrWithNew(QContext.getQInstance()).getAdditionalRouteProviderReferences()).hasSize(1);
      Javalin server = Javalin.create(routes::acceptJavalinConfig).start(0);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         String base = "http://localhost:" + server.port() + "/qqq/hooks/crm/";
         HttpResponse<String> get = request(client, base + "confirm/" + confirm, "GET");
         assertThat(get.statusCode()).isEqualTo(200);
         assertThat(get.body()).contains("<button", "method=\"post\"").doesNotContain("private@example.test", confirm);
         assertThat(get.headers().firstValue("Cache-Control")).contains("no-store");
         assertThat(CrmStore.asUser().get("crm_email_list_member", memberId).getValueInteger("status")).isEqualTo(5);
         assertThat(request(client, base + "confirm/" + confirm, "POST").statusCode()).isEqualTo(200);
         assertThat(CrmStore.asUser().get("crm_email_list_member", memberId).getValueInteger("status")).isEqualTo(1);
         String unsubscribe = tokens.issue("unsubscribe", contactId, listId);
         assertThat(request(client, base + "unsubscribe/" + unsubscribe, "GET").statusCode()).isEqualTo(200);
         assertThat(CrmStore.asUser().get("crm_email_list_member", memberId).getValueInteger("status")).isEqualTo(1);
         assertThat(request(client, base + "unsubscribe/" + unsubscribe, "POST").statusCode()).isEqualTo(200);
         assertThat(CrmStore.asUser().get("crm_email_list_member", memberId).getValueInteger("status")).isEqualTo(2);
         for(String method : java.util.List.of("GET", "POST"))
         {
            assertThat(request(client, base + "confirm/invalid", method).statusCode()).isEqualTo(400);
            assertThat(request(client, base + "unsubscribe/" + confirm, method).statusCode()).isEqualTo(400);
            String expired = new CrmTokens(config, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)).issue("unsubscribe", contactId, null);
            assertThat(request(client, base + "unsubscribe/" + expired, method).statusCode()).isEqualTo(400);
         }
      }
      finally
      {
         server.stop();
      }
   }

   /*******************************************************************************
    ** Send a browser-style request to the machine route.
    *******************************************************************************/
   private HttpResponse<String> request(HttpClient client, String url, String method) throws Exception
   {
      return (client.send(HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()));
   }
}
