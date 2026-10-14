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
 ** Tests for CRM choices scoped by another editable field on the same form.
 *******************************************************************************/

package com.kingsrook.qbits.crm;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.activities.model.ActivityOutcome;
import com.kingsrook.qbits.crm.activities.processes.LogActivityProcess;
import com.kingsrook.qbits.crm.core.model.EmailListMember;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.deals.model.PipelineStage;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise the generated metadata and real possible-value searches in memory.
 *******************************************************************************/
class ContingentPossibleValuesTest extends BaseTest
{
   /*******************************************************************************
    ** Every contingent field must point to an existing scope column and retain
    ** its template after searches for either scope and either empty-input mode.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({
      Deal.TABLE_NAME + ", pipelineId, pipelineStageId, " + PipelineStage.TABLE_NAME,
      Activity.TABLE_NAME + ", activityTypeId, outcomeId, " + ActivityOutcome.TABLE_NAME,
      EmailListMember.TABLE_NAME + ", contactId, formSubmissionId, " + FormSubmission.TABLE_NAME
   })
   void testContingentSearch(String tableName, String parent, String child, String sourceTable) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      QFieldMetaData field = table.getField(child);
      assertThat(table.getField(parent).getIsEditable()).isTrue();
      assertThat(field.getIsEditable()).isTrue();
      assertThat(QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName()).getTableName()).isEqualTo(sourceTable);
      assertThat(QContext.getQInstance().getTable(sourceTable).getField(parent)).isNotNull();

      QQueryFilter template = field.getPossibleValueSourceFilter();
      assertThat(template).isNotNull();
      QFilterCriteria expected = new QFilterCriteria(parent, QCriteriaOperator.EQUALS, "${input." + parent + "}");
      assertThat(template.getCriteria()).containsExactly(expected);

      //////////////////////////////////////////////////////////////////////////////////
      // Seed the search backend directly so insert customizers cannot relink scopes. //
      //////////////////////////////////////////////////////////////////////////////////
      MemoryRecordStore.getInstance().insert(new InsertInput(sourceTable).withRecords(List.of(
         scopedRecord(11, parent, 1), scopedRecord(12, parent, 1), scopedRecord(21, parent, 2))), false);

      assertThat(search(field, Map.of(parent, 1), PossibleValueSearchFilterUseCase.FORM)).containsExactlyInAnyOrder(11, 12);
      assertThat(search(field, Map.of(parent, 2), PossibleValueSearchFilterUseCase.FORM)).containsExactly(21);
      assertThat(search(field, Map.of(), PossibleValueSearchFilterUseCase.FORM)).isEmpty();
      assertThat(search(field, Map.of(), PossibleValueSearchFilterUseCase.FILTER)).containsExactlyInAnyOrder(11, 12, 21);
      assertThat(template.getCriteria()).containsExactly(expected);
   }



   /*******************************************************************************
    ** Optional company links and backend-only process inputs stay unrestricted.
    *******************************************************************************/
   @Test
   void testSkippedFields()
   {
      QTableMetaData activity = QContext.getQInstance().getTable(Activity.TABLE_NAME);
      assertThat(activity.getField("companyId").getIsRequired()).isFalse();
      assertThat(activity.getField("contactId").getPossibleValueSourceFilter()).isNull();
      assertThat(activity.getField("dealId").getPossibleValueSourceFilter()).isNull();
      assertThat(QContext.getQInstance().getProcess(LogActivityProcess.NAME).getStepList())
         .allMatch(step -> step instanceof QBackendStepMetaData);
   }



   /*******************************************************************************
    ** Build a labeled row usable by each of the three table possible-value sources.
    *******************************************************************************/
   private QRecord scopedRecord(int id, String parent, int scope)
   {
      return new QRecord().withValue("id", id).withValue(parent, scope)
         .withValue("name", "Choice " + id).withValue("firstName", "Contact " + id)
         .withValue("email", "contact" + id + "@example.com").withValue("formKey", "test-form").withValue("formType", 1);
   }



   /*******************************************************************************
    ** Interpret a cloned field filter just as a frontend form or filter does.
    *******************************************************************************/
   private List<Object> search(QFieldMetaData field, Map<String, Serializable> values, PossibleValueSearchFilterUseCase useCase) throws QException
   {
      QQueryFilter filter = field.getPossibleValueSourceFilter().clone();
      filter.interpretValues(MapBuilder.of("input", values), useCase);
      return new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput()
         .withPossibleValueSourceName(field.getPossibleValueSourceName())
         .withDefaultQueryFilter(filter)).getResults().stream().<Object>map(QPossibleValue::getId).toList();
   }
}
