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


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
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
