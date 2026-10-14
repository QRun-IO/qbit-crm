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


import java.time.Instant;
import java.util.Optional;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qbits.crm.core.model.enums.CrmEmailStatus;
import com.kingsrook.qbits.email.api.EmailApi;
import com.kingsrook.qbits.email.api.EmailApiCategory;
import com.kingsrook.qbits.email.api.EmailApiEffectOutcome;
import com.kingsrook.qbits.email.api.EmailApiRequest;
import com.kingsrook.qqq.backend.core.actions.messaging.SendMessageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;


/*******************************************************************************
 ** Only CRM adapter that imports the optional qbit-email public API; otherwise delegates to core messaging.
 *******************************************************************************/
public class CrmEmailTransport
{
   private final CrmQBitConfig config;

   /*******************************************************************************
    ** Configure the transport for this instance. Subclass send for a host/test transport.
    *******************************************************************************/
   public CrmEmailTransport(CrmQBitConfig config)
   {
      this.config = config;
   }

   /*******************************************************************************
    ** Check class presence before linking the optional public API.
    *******************************************************************************/
   private boolean emailInstalled()
   {
      try
      {
         Class.forName("com.kingsrook.qbits.email.api.EmailApi");
         return (EmailApi.isInstalled(QContext.getQInstance()));
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         return (false);
      }
   }

   /*******************************************************************************
    ** Report a usable configured transport. An installed but unready email qbit is not bypassed.
    *******************************************************************************/
   public CrmReadiness readiness() throws QException
   {
      if(emailInstalled())
      {
         return (EmailApi.readiness(QContext.getQInstance(), EmailApiCategory.MARKETING).ready()
            ? CrmReadiness.QBIT_EMAIL : CrmReadiness.NOT_CONFIGURED);
      }
      return (config.getMessagingProviderName() != null && QContext.getQInstance().getMessagingProvider(config.getMessagingProviderName()) != null
         ? CrmReadiness.CORE_MESSAGING : CrmReadiness.NOT_CONFIGURED);
   }

   /*******************************************************************************
    ** Deliver one message, using a stable CRM purpose/id key for the durable email outbox.
    *******************************************************************************/
   public void send(String purpose, String id, Integer contactId, String address, String subject, String body, boolean marketing) throws QException
   {
      deliver(purpose, id, contactId, address, subject, body, marketing);
   }

   /*******************************************************************************
    ** Return the transport result without guessing that queued email was sent.
    *******************************************************************************/
   private Delivery deliver(String purpose, String id, Integer contactId, String address, String subject, String body, boolean marketing) throws QException
   {
      if(emailInstalled())
      {
         String key = "crm:" + purpose + ":" + id;
         var result = EmailApi.sendOnce(new EmailApiRequest().withTo(address).withSubject(subject).withMarkdownBody(body)
            .withCategory(marketing ? EmailApiCategory.MARKETING : EmailApiCategory.TRANSACTIONAL)
            .withLinkedRecord("crm_contact", contactId).withSender("crm", purpose).withIdempotencyKey(key), key);
         if(result.outcome() == EmailApiEffectOutcome.REFUSED)
         {
            throw (new QUserFacingException("The email service could not accept this message."));
         }
         var message = result.message();
         return (message == null ? new Delivery(null, CrmEmailStatus.QUEUED, null, null, null, null, null)
            : new Delivery(String.valueOf(message.id()), status(message.status()), message.sentDate(), message.deliveredDate(), null, null, null));
      }
      if(readiness() != CrmReadiness.CORE_MESSAGING)
      {
         throw (new QUserFacingException("CRM email is not configured. Install qbit-email or configure a messaging provider."));
      }
      new SendMessageAction().execute(new SendMessageInput().withMessagingProviderName(config.getMessagingProviderName())
         .withTo(new Party().withAddress(address).withRole(EmailPartyRole.TO)).withSubject(subject)
         .withContent(new Content().withBody(body).withContentRole(EmailContentRole.TEXT)));
      return (new Delivery(null, CrmEmailStatus.SENT, Instant.now(), null, null, null, null));
   }

   /*******************************************************************************
    ** CRM-owned delivery snapshot; optional email types never escape this adapter.
    *******************************************************************************/
   public record Delivery(String messageId, CrmEmailStatus status, Instant sentDate, Instant deliveredDate,
                          Instant openedDate, Instant clickedDate, Instant bouncedDate)
   {
   }

   /*******************************************************************************
    ** Send and return the actual outbox state, or the successful core send state.
    *******************************************************************************/
   public Delivery sendMessage(String purpose, String id, Integer contactId, String address, String subject, String body, boolean marketing) throws QException
   {
      send(purpose, id, contactId, address, subject, body, marketing);
      return (lookup("crm:" + purpose + ":" + id).orElseGet(() ->
         new Delivery(null, CrmEmailStatus.SENT, Instant.now(), null, null, null, null)));
   }

   /*******************************************************************************
    ** Read delivery through the published API only. RC.3 exposes sent/delivered dates.
    *******************************************************************************/
   public Optional<Delivery> lookup(String key) throws QException
   {
      if(!emailInstalled() || key == null)
      {
         return (Optional.empty());
      }
      return (EmailApi.getMessageByIdempotencyKey(key).map(message -> new Delivery(String.valueOf(message.id()),
         status(message.status()), message.sentDate(), message.deliveredDate(), null, null, null)));
   }

   /*******************************************************************************
    ** Preserve known delivery states and leave unknown/pending states eligible for sync.
    *******************************************************************************/
   private CrmEmailStatus status(String status)
   {
      try
      {
         return (CrmEmailStatus.valueOf(status));
      }
      catch(IllegalArgumentException | NullPointerException e)
      {
         return (CrmEmailStatus.QUEUED);
      }
   }
}
