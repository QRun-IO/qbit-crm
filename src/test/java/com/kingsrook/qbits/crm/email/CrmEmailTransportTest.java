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

package com.kingsrook.qbits.crm.email;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qbits.email.EmailQBitConfig;
import com.kingsrook.qbits.email.EmailQBitProducer;
import com.kingsrook.qbits.email.api.EmailApi;
import com.kingsrook.qbits.email.api.EmailApiCategory;
import com.kingsrook.qbits.email.api.EmailApiTemplate;
import com.kingsrook.qbits.email.permissions.EmailPermissions;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderInterface;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Core fallback exercises the real SendMessageAction and provider dispatcher without sending external mail.
 *******************************************************************************/
public class CrmEmailTransportTest extends BaseTest
{
   /*******************************************************************************
    ** A named core provider receives both transactional and marketing messages.
    *******************************************************************************/
   @Test
   void configuredCoreProviderReceivesMessages() throws Exception
   {
      QMessagingProviderDispatcher.registerMessagingProvider(new RecordingProvider());
      QContext.getQInstance().addMessagingProvider(new QMessagingProviderMetaData().withName("crm-test").withType("CRM_TEST"));
      var config = CrmApi.config().withMessagingProviderName("crm-test");
      CrmEmailTransport transport = new CrmEmailTransport(config);
      assertThat(transport.readiness()).isEqualTo(CrmReadiness.CORE_MESSAGING);
      assertThat(CrmApi.readiness()).isEqualTo(CrmReadiness.CORE_MESSAGING);
      transport.send("one-to-one", "42", 1, "test@example.test", "Hello", "A reply", false);
      assertThat(RecordingProvider.last.getMessagingProviderName()).isEqualTo("crm-test");
      assertThat(RecordingProvider.last.getSubject()).isEqualTo("Hello");
      transport.send("sequence", "43", 1, "test@example.test", "Next step", "A sequence", true);
      assertThat(RecordingProvider.last.getSubject()).isEqualTo("Next step");
      RecordingProvider.last = null;
   }

   /*******************************************************************************
    ** Shared mode selects the email-owned template and records the final subject.
    *******************************************************************************/
   @Test
   void sharedTemplateUsesOwnerAndExactSubject() throws Exception
   {
      var instance = QContext.getQInstance();
      new EmailQBitProducer().withQBitConfig(new EmailQBitConfig()
         .withBackendName(BACKEND_NAME).withTemplatePermissionBaseName("crm.manage")).produce(instance);
      new QInstanceValidator().validate(instance);
      Set<String> permissions = new HashSet<>(CrmPermissions.ALL);
      permissions.addAll(EmailPermissions.ALL);
      QContext.getQSession().setPermissions(permissions);
      new InsertAction().execute(new InsertInput("email_provider")
         .withRecord(new QRecord().withValue("name", "test").withValue("label", "Test").withValue("kind", "SMTP")
            .withValue("host", "127.0.0.1").withValue("port", 25).withValue("security", "NONE").withValue("fromAddress", "sender@example.test").withValue("enabled", true).withValue("isDefault", true)));
      EmailApi.ensureTemplate(new EmailApiTemplate().withName("shared")
         .withLabel("Shared").withKind("HTML").withCategory(EmailApiCategory.TRANSACTIONAL)
         .withSubject("Message {{message.id}}").withBody("<p>Native HTML</p>"));
      var templates = new QueryAction().execute(new QueryInput("email_template")).getRecords();
      var transport = new CrmEmailTransport(CrmApi.config().withSharedEmailTemplates(true));
      var first = transport.sendTemplateMessage("manual", "42", 1, "recipient@example.test", templates.getFirst().getValueInteger("id"), null, null, false, Map.of());
      assertThat(first.subject()).isEqualTo("Message " + first.delivery().messageId());
      var repeated = transport.sendTemplateMessage("manual", "42", 1, "recipient@example.test", templates.getFirst().getValueInteger("id"), null, null, false, Map.of());
      assertThat(repeated).isEqualTo(first);
   }

   /*******************************************************************************
    ** Public constructor allows the core dispatcher to instantiate this provider.
    *******************************************************************************/
   public static class RecordingProvider implements MessagingProviderInterface
   {
      private static SendMessageInput last;

      @Override
      public String getType()
      {
         return ("CRM_TEST");
      }

      @Override
      public SendMessageOutput sendMessage(SendMessageInput input)
      {
         last = input;
         return (new SendMessageOutput());
      }
   }
}
