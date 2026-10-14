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


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Contact record timeline includes all four sources with no cross-contact rows.
 *******************************************************************************/
class CrmContactTimelineWidgetTest extends BaseTest
{
   /*******************************************************************************
    ** The record-view id selects and merges the four dated sources.
    *******************************************************************************/
   @Test
   void timeline() throws Exception
   {
      CrmHomeWidgetsTest.seed("crm_contact", new QRecord().withValue("id", 1));
      int days = 0;
      for(String table : List.of("crm_activity", "crm_email_message", "crm_form_submission", "crm_consent_record"))
      {
         CrmHomeWidgetsTest.seed(table, new QRecord().withValue("contactId", 1).withValue("createDate", Instant.now().minus(days++, ChronoUnit.DAYS)));
         CrmHomeWidgetsTest.seed(table, new QRecord().withValue("contactId", 2).withValue("createDate", Instant.now()));
      }
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget("crm_contact_timeline"));
      input.addQueryParam("id", "1");
      TableData data = (TableData) new CrmContactTimelineRenderer().render(input).getWidgetData();
      assertThat(data.getRows()).hasSize(4);
      CrmHomeWidgetsTest.assertCell(data, 0, "second", "Activity");
      CrmHomeWidgetsTest.assertCell(data, 1, "second", "Email message");
      CrmHomeWidgetsTest.assertCell(data, 2, "second", "Web lead");
      CrmHomeWidgetsTest.assertCell(data, 3, "second", "Consent record");
      assertThat(data.getColumns()).hasSize(3);
   }


   /*******************************************************************************
    ** Missing and nonexistent parent ids never query all contacts' histories.
    *******************************************************************************/
   @Test
   void noParent() throws Exception
   {
      RenderWidgetInput input = new RenderWidgetInput();
      assertThat(((TableData) new CrmContactTimelineRenderer().render(input).getWidgetData()).getRows()).isEmpty();
      input.addQueryParam("id", "999");
      assertThat(((TableData) new CrmContactTimelineRenderer().render(input).getWidgetData()).getRows()).isEmpty();
   }
}
