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


package com.kingsrook.qbits.crm.widgets;


import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** A contact's latest fifty recorded events across activities, sent messages,
 ** web leads and consent. Each source is bounded before merging by create date.
 *******************************************************************************/
public class CrmContactTimelineRenderer extends AbstractWidgetRenderer
{
   /*******************************************************************************
    ** Record sections supply id; contactId remains useful for embedded hosts.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      String id = input.getQueryParams().getOrDefault("id", input.getQueryParams().get("contactId"));
      if(id != null && !id.isBlank() && GetAction.execute("crm_contact", Integer.valueOf(id)) != null)
      {
         List<QRecord> events = new ArrayList<>();
         for(String table : List.of("crm_activity", "crm_email_message", "crm_form_submission", "crm_consent_record"))
         {
            for(QRecord record : CrmHomeRenderer.query(table, CrmHomeRenderer.equalsFilter("contactId", Integer.valueOf(id)), "createDate", false, 50))
            {
               record.setValue("timelineTable", table);
               events.add(record);
            }
         }
         events.sort(Comparator.comparing((QRecord record) -> Objects.requireNonNullElse(record.getValueInstant("createDate"), Instant.EPOCH)).reversed()
            .thenComparing(record -> record.getValueString("timelineTable")).thenComparing(record -> record.getValueInteger("id")));
         for(QRecord record : events.stream().limit(50).toList())
         {
            String table = record.getValueString("timelineTable");
            String kind = Map.of("crm_activity", "Activity", "crm_email_message", "Email message",
               "crm_form_submission", "Web lead", "crm_consent_record", "Consent record").get(table);
            String detail = Objects.toString(record.getValue("subject"), Objects.toString(record.getValue("email"), kind));
            rows.add(CrmHomeRenderer.row(AiLinkBuilder.recordHref(table, record.getValue("id")), formatDate(record.getValueInstant("createDate")), kind, detail));
         }
      }
      return (new RenderWidgetOutput(CrmHomeRenderer.table(rows, "Recorded", "Kind", "Details")));
   }


   /*******************************************************************************
    ** Prefer the session timezone (including DST), then offset, otherwise UTC.
    *******************************************************************************/
   static String formatDate(Instant instant)
   {
      if(instant == null)
      {
         return ("");
      }
      ZoneId zone = ZoneOffset.UTC;
      QSession session = QContext.getQSession();
      String timezone = session == null ? null : session.getValue(QSession.VALUE_KEY_USER_TIMEZONE);
      String offset = session == null ? null : session.getValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES);
      try
      {
         if(timezone != null && !timezone.isBlank())
         {
            zone = ZoneId.of(timezone);
         }
         else if(offset != null && !offset.isBlank())
         {
            zone = ZoneOffset.ofTotalSeconds(Math.multiplyExact(60, Integer.parseInt(offset)));
         }
      }
      catch(DateTimeException | IllegalArgumentException | ArithmeticException e)
      {
         zone = ZoneOffset.UTC;
      }
      return (DateTimeFormatter.ofPattern("MMM d, uuuu h:mm a", Locale.US).withZone(zone).format(instant));
   }
}
