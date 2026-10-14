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
 ** Coverage tests for FormSubmission entity -- exercises every getter/setter/withX.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercises every fluent setter and getter on FormSubmission.
 *******************************************************************************/
class FormSubmissionCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** Test all fluent setters and getters.
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      Instant now = Instant.now();

      FormSubmission form = new FormSubmission()
         .withId(1)
         .withFormKey("test-form").withFormType(1)
         .withEmail("lead@example.com")
         .withFirstName("Lead")
         .withLastName("User")
         .withCompany("LeadCo")
         .withPhone("555-9999")
         .withMessage("I want a demo")
         .withSourceUrl("https://example.com/landing")
         .withUtmSource("google")
         .withUtmMedium("cpc")
         .withUtmCampaign("spring-sale")
         .withUtmTerm("crm software")
         .withUtmContent("banner-ad")
         .withReferrer("https://google.com")
         .withContactId(42)
         .withConversionStatus(2)
         .withConvertedDate(now)
         .withConvertedByUserId("user-001")
         .withMetadata("{\"key\":\"value\"}")
         .withCreateDate(now)
         .withModifyDate(now);

      assertThat(form.getId()).isEqualTo(1);
      assertThat(form.getFormType()).isEqualTo(1);
      assertThat(form.getEmail()).isEqualTo("lead@example.com");
      assertThat(form.getFirstName()).isEqualTo("Lead");
      assertThat(form.getLastName()).isEqualTo("User");
      assertThat(form.getCompany()).isEqualTo("LeadCo");
      assertThat(form.getPhone()).isEqualTo("555-9999");
      assertThat(form.getMessage()).isEqualTo("I want a demo");
      assertThat(form.getSourceUrl()).isEqualTo("https://example.com/landing");
      assertThat(form.getUtmSource()).isEqualTo("google");
      assertThat(form.getUtmMedium()).isEqualTo("cpc");
      assertThat(form.getUtmCampaign()).isEqualTo("spring-sale");
      assertThat(form.getUtmTerm()).isEqualTo("crm software");
      assertThat(form.getUtmContent()).isEqualTo("banner-ad");
      assertThat(form.getReferrer()).isEqualTo("https://google.com");
      assertThat(form.getContactId()).isEqualTo(42);
      assertThat(form.getConversionStatus()).isEqualTo(2);
      assertThat(form.getConvertedDate()).isEqualTo(now);
      assertThat(form.getConvertedByUserId()).isEqualTo("user-001");
      assertThat(form.getMetadata()).isEqualTo("{\"key\":\"value\"}");
      assertThat(form.getCreateDate()).isEqualTo(now);
      assertThat(form.getModifyDate()).isEqualTo(now);
   }



   /*******************************************************************************
    ** Test all plain setters.
    *******************************************************************************/
   @Test
   void testPlainSetters()
   {
      Instant now = Instant.now();
      FormSubmission form = new FormSubmission();

      form.setId(99);
      form.setFormType(2);
      form.setEmail("other@example.com");
      form.setFirstName("Other");
      form.setLastName("Person");
      form.setCompany("OtherCo");
      form.setPhone("555-8888");
      form.setMessage("Hello");
      form.setSourceUrl("https://other.com");
      form.setUtmSource("facebook");
      form.setUtmMedium("social");
      form.setUtmCampaign("fall-promo");
      form.setUtmTerm("automation");
      form.setUtmContent("carousel");
      form.setReferrer("https://facebook.com");
      form.setContactId(55);
      form.setConversionStatus(3);
      form.setConvertedDate(now);
      form.setConvertedByUserId("user-002");
      form.setMetadata("{\"foo\":\"bar\"}");
      form.setCreateDate(now);
      form.setModifyDate(now);

      assertThat(form.getId()).isEqualTo(99);
      assertThat(form.getFormType()).isEqualTo(2);
      assertThat(form.getEmail()).isEqualTo("other@example.com");
      assertThat(form.getFirstName()).isEqualTo("Other");
      assertThat(form.getLastName()).isEqualTo("Person");
      assertThat(form.getCompany()).isEqualTo("OtherCo");
      assertThat(form.getPhone()).isEqualTo("555-8888");
      assertThat(form.getMessage()).isEqualTo("Hello");
      assertThat(form.getSourceUrl()).isEqualTo("https://other.com");
      assertThat(form.getUtmSource()).isEqualTo("facebook");
      assertThat(form.getUtmMedium()).isEqualTo("social");
      assertThat(form.getUtmCampaign()).isEqualTo("fall-promo");
      assertThat(form.getUtmTerm()).isEqualTo("automation");
      assertThat(form.getUtmContent()).isEqualTo("carousel");
      assertThat(form.getReferrer()).isEqualTo("https://facebook.com");
      assertThat(form.getContactId()).isEqualTo(55);
      assertThat(form.getConversionStatus()).isEqualTo(3);
      assertThat(form.getConvertedDate()).isEqualTo(now);
      assertThat(form.getConvertedByUserId()).isEqualTo("user-002");
      assertThat(form.getMetadata()).isEqualTo("{\"foo\":\"bar\"}");
      assertThat(form.getCreateDate()).isEqualTo(now);
      assertThat(form.getModifyDate()).isEqualTo(now);
   }

}
