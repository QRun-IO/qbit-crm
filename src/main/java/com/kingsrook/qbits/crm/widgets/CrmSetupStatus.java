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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmReadiness;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** The Setup card on the CRM home: the configuration the CRM's processes depend
 ** on, whether it is in place, and the next step. Reads through core actions, so
 ** the signed-in user's permissions and tenant locks apply.
 *******************************************************************************/
public final class CrmSetupStatus
{
   /*******************************************************************************
    ** OK: in place. TODO: something the CRM needs is missing. OPTIONAL: a feature
    ** that stays off until it is set up.
    *******************************************************************************/
   public enum State
   {
      OK("Set up"),
      TODO("Needs setup"),
      OPTIONAL("Optional");

      private final String label;



      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      State(String label)
      {
         this.label = label;
      }



      /*******************************************************************************
       ** Getter for label.
       *******************************************************************************/
      public String getLabel()
      {
         return (label);
      }
   }



   /*******************************************************************************
    ** One line of the card.
    *******************************************************************************/
   public record Item(String label, String tableName, State state, String detail, String nextStep)
   {
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmSetupStatus()
   {
   }



   /*******************************************************************************
    ** Build the items for the current user.
    *******************************************************************************/
   public static List<Item> load() throws QException
   {
      List<Item> items = new ArrayList<>();

      ///////////////////////////////////////////////////////////////////
      // pipeline and stages: Convert lead needs a default pipeline    //
      // with at least one stage                                       //
      ///////////////////////////////////////////////////////////////////
      List<QRecord> defaults = CrmHomeRenderer.query("crm_pipeline", CrmHomeRenderer.equalsFilter("isDefault", true), "id", true, 1);
      if(defaults.isEmpty())
      {
         items.add(new Item("Default pipeline", "crm_pipeline", State.TODO, "None: Convert lead fails without one.",
            "Add a pipeline and tick Default (or include the crm-defaults changelog)."));
         items.add(new Item("Pipeline stages", "crm_pipeline_stage", State.TODO, "A deal needs a pipeline with stages.", "Add its stages once the pipeline exists."));
      }
      else
      {
         items.add(new Item("Default pipeline", "crm_pipeline", State.OK, defaults.getFirst().getValueString("name") + " is the default.", null));
         List<QRecord> stages = CrmHomeRenderer.query("crm_pipeline_stage", CrmHomeRenderer.equalsFilter("pipelineId", defaults.getFirst().getValueInteger("id")), "sortOrder", true, 200);
         boolean won = stages.stream().anyMatch(stage -> Boolean.TRUE.equals(stage.getValueBoolean("isClosedWon")));
         boolean lost = stages.stream().anyMatch(stage -> Boolean.TRUE.equals(stage.getValueBoolean("isClosedLost")));
         if(stages.isEmpty())
         {
            items.add(new Item("Pipeline stages", "crm_pipeline_stage", State.TODO, "The default pipeline has no stages: Convert lead fails.",
               "Add its stages, in order, with a win probability each."));
         }
         else if(!won || !lost)
         {
            items.add(new Item("Pipeline stages", "crm_pipeline_stage", State.TODO, stages.size() + " stage" + (stages.size() == 1 ? "" : "s") + ", but no " + (won ? "Closed Lost" : "Closed Won") + " stage.",
               "Tick Closed Won and Closed Lost on the stages that end a deal: the home, goals and reports count deals from them."));
         }
         else
         {
            items.add(new Item("Pipeline stages", "crm_pipeline_stage", State.OK, stages.size() + " stages in the default pipeline.", null));
         }
      }

      ////////////////////////////////////////////////////////////////////////
      // lifecycle stages: the CRM looks up "Lead" and "Opportunity" by name //
      ////////////////////////////////////////////////////////////////////////
      List<String> missing = new ArrayList<>();
      for(String name : List.of("Lead", "Opportunity"))
      {
         if(CrmHomeRenderer.query("crm_lifecycle_stage", CrmHomeRenderer.equalsFilter("name", name), "id", true, 1).isEmpty())
         {
            missing.add(name);
         }
      }
      if(missing.isEmpty())
      {
         items.add(new Item("Lifecycle stages", "crm_lifecycle_stage", State.OK, "Lead and Opportunity exist.", null));
      }
      else
      {
         String effect = missing.size() == 2 ? "new web leads get no stage and Convert lead does not move the contact"
            : missing.contains("Lead") ? "new web leads get no stage" : "Convert lead does not move the contact";
         items.add(new Item("Lifecycle stages", "crm_lifecycle_stage", State.TODO, "Missing " + String.join(" and ", missing) + ": " + effect + ".",
            "Add " + (missing.size() == 2 ? "stages" : "a stage") + " named exactly " + String.join(" and ", missing.stream().map(name -> "\"" + name + "\"").toList()) + "."));
      }

      ///////////////////////////////////////////////////////////
      // currency: amounts need a base currency for the prefix //
      ///////////////////////////////////////////////////////////
      List<QRecord> base = CrmHomeRenderer.query("crm_currency", CrmHomeRenderer.equalsFilter("isBaseCurrency", true), "id", true, 1);
      items.add(base.isEmpty()
         ? new Item("Base currency", "crm_currency", State.TODO, "None: amounts have no currency prefix and conversions do nothing.", "Add your currency with Base currency ticked and rate 1.")
         : new Item("Base currency", "crm_currency", State.OK, base.getFirst().getValueString("currencyCode") + " is the base currency.", null));

      ///////////////////////////////////////////////////////
      // email: sequences and list mail need a delivery path //
      ///////////////////////////////////////////////////////
      CrmReadiness readiness = CrmApi.readiness();
      items.add(readiness == CrmReadiness.NOT_CONFIGURED
         ? new Item("Email delivery", "crm_email_template", State.OPTIONAL, "No email path: sequences and list email cannot send.",
            "Install and configure the email qbit, or set a messaging provider on the CRM configuration.")
         : new Item("Email delivery", "crm_email_template", State.OK, readiness == CrmReadiness.QBIT_EMAIL ? "Sends through the email qbit." : "Sends through core messaging.", null));
      return (items);
   }

}
