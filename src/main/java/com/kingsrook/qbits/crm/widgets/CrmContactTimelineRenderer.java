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


/*******************************************************************************
 ** Renderer for the CRM Contact Timeline widget.
 **
 ** Queries activities for a specific contactId (from widget input) and returns
 ** a StepperData with each activity as a chronological step. Activities are
 ** ordered by createDate ascending.
 *******************************************************************************/

package com.kingsrook.qbits.crm.widgets;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StepperData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Renders a contact's activity timeline as a stepper widget.
 *******************************************************************************/
public class CrmContactTimelineRenderer extends AbstractWidgetRenderer
{
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm")
      .withZone(ZoneId.systemDefault());



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      String contactIdParam = input.getQueryParams().get("contactId");

      if(!StringUtils.hasContent(contactIdParam))
      {
         return (new RenderWidgetOutput(new StepperData("Contact Timeline", 0, List.of())));
      }

      Integer contactId = ValueUtils.getValueAsInteger(contactIdParam);

      //////////////////////////////////////////////////////////////
      // query activities for this contact, ordered chronologically //
      //////////////////////////////////////////////////////////////
      QueryInput queryInput = new QueryInput(Activity.TABLE_NAME);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId))
         .withOrderBy(new QFilterOrderBy("createDate", true)));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ////////////////////////////////////
      // build steps from activities    //
      ////////////////////////////////////
      List<StepperData.Step> steps = new ArrayList<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String subject = record.getValueString("subject");
         if(subject == null)
         {
            subject = "Activity";
         }

         Instant createDate = record.getValueInstant("createDate");
         String dateStr = createDate != null ? DATE_FORMATTER.format(createDate) : "";

         String description = record.getValueString("description");
         String linkText = StringUtils.hasContent(description)
            ? description.substring(0, Math.min(description.length(), 100))
            : dateStr;

         steps.add(new StepperData.Step()
            .withLabel(subject)
            .withLinkText(linkText));
      }

      //////////////////////////////////////////////////////////////
      // set activeStep to the last step (most recent activity)   //
      //////////////////////////////////////////////////////////////
      Integer activeStep = steps.isEmpty() ? 0 : steps.size() - 1;

      StepperData stepperData = new StepperData("Contact Timeline", activeStep, steps);

      return (new RenderWidgetOutput(stepperData));
   }

}
