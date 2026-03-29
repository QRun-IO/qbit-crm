/*******************************************************************************
 ** Test class for all CRM PossibleValueSource enums. Validates NAME constants,
 ** getById() lookups, null/unknown ID handling, and PossibleValueEnum interface
 ** contract for every enum value across all 24 CRM enums.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for all CRM PossibleValueEnum types.
 *******************************************************************************/
class CrmEnumPvsTest
{

   /*******************************************************************************
    ** Test CrmEntityType
    *******************************************************************************/
   @Test
   void testCrmEntityType()
   {
      assertThat(CrmEntityType.NAME).isNotNull();
      assertThat(CrmEntityType.getById(null)).isNull();
      assertThat(CrmEntityType.getById(999)).isNull();

      assertEnumValue(CrmEntityType.CONTACT, 1, "Contact");
      assertEnumValue(CrmEntityType.COMPANY, 2, "Company");
      assertEnumValue(CrmEntityType.DEAL, 3, "Deal");
      assertEnumValue(CrmEntityType.ACTIVITY, 4, "Activity");

      for(CrmEntityType value : CrmEntityType.values())
      {
         assertThat(CrmEntityType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmCompanyType
    *******************************************************************************/
   @Test
   void testCrmCompanyType()
   {
      assertThat(CrmCompanyType.NAME).isNotNull();
      assertThat(CrmCompanyType.getById(null)).isNull();
      assertThat(CrmCompanyType.getById(999)).isNull();

      assertEnumValue(CrmCompanyType.PROSPECT, 1, "Prospect");
      assertEnumValue(CrmCompanyType.CUSTOMER, 2, "Customer");
      assertEnumValue(CrmCompanyType.PARTNER, 3, "Partner");
      assertEnumValue(CrmCompanyType.COMPETITOR, 4, "Competitor");
      assertEnumValue(CrmCompanyType.OTHER, 5, "Other");

      for(CrmCompanyType value : CrmCompanyType.values())
      {
         assertThat(CrmCompanyType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmDealPriority
    *******************************************************************************/
   @Test
   void testCrmDealPriority()
   {
      assertThat(CrmDealPriority.NAME).isNotNull();
      assertThat(CrmDealPriority.getById(null)).isNull();
      assertThat(CrmDealPriority.getById(999)).isNull();

      assertEnumValue(CrmDealPriority.LOW, 1, "Low");
      assertEnumValue(CrmDealPriority.MEDIUM, 2, "Medium");
      assertEnumValue(CrmDealPriority.HIGH, 3, "High");
      assertEnumValue(CrmDealPriority.CRITICAL, 4, "Critical");

      for(CrmDealPriority value : CrmDealPriority.values())
      {
         assertThat(CrmDealPriority.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmActivityPriority
    *******************************************************************************/
   @Test
   void testCrmActivityPriority()
   {
      assertThat(CrmActivityPriority.NAME).isNotNull();
      assertThat(CrmActivityPriority.getById(null)).isNull();
      assertThat(CrmActivityPriority.getById(999)).isNull();

      assertEnumValue(CrmActivityPriority.LOW, 1, "Low");
      assertEnumValue(CrmActivityPriority.MEDIUM, 2, "Medium");
      assertEnumValue(CrmActivityPriority.HIGH, 3, "High");

      for(CrmActivityPriority value : CrmActivityPriority.values())
      {
         assertThat(CrmActivityPriority.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmDirection
    *******************************************************************************/
   @Test
   void testCrmDirection()
   {
      assertThat(CrmDirection.NAME).isNotNull();
      assertThat(CrmDirection.getById(null)).isNull();
      assertThat(CrmDirection.getById(999)).isNull();

      assertEnumValue(CrmDirection.INBOUND, 1, "Inbound");
      assertEnumValue(CrmDirection.OUTBOUND, 2, "Outbound");

      for(CrmDirection value : CrmDirection.values())
      {
         assertThat(CrmDirection.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmBounceType
    *******************************************************************************/
   @Test
   void testCrmBounceType()
   {
      assertThat(CrmBounceType.NAME).isNotNull();
      assertThat(CrmBounceType.getById(null)).isNull();
      assertThat(CrmBounceType.getById(999)).isNull();

      assertEnumValue(CrmBounceType.HARD, 1, "Hard");
      assertEnumValue(CrmBounceType.SOFT, 2, "Soft");

      for(CrmBounceType value : CrmBounceType.values())
      {
         assertThat(CrmBounceType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmWinLossType
    *******************************************************************************/
   @Test
   void testCrmWinLossType()
   {
      assertThat(CrmWinLossType.NAME).isNotNull();
      assertThat(CrmWinLossType.getById(null)).isNull();
      assertThat(CrmWinLossType.getById(999)).isNull();

      assertEnumValue(CrmWinLossType.WIN, 1, "Win");
      assertEnumValue(CrmWinLossType.LOSS, 2, "Loss");

      for(CrmWinLossType value : CrmWinLossType.values())
      {
         assertThat(CrmWinLossType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmParticipantRole
    *******************************************************************************/
   @Test
   void testCrmParticipantRole()
   {
      assertThat(CrmParticipantRole.NAME).isNotNull();
      assertThat(CrmParticipantRole.getById(null)).isNull();
      assertThat(CrmParticipantRole.getById(999)).isNull();

      assertEnumValue(CrmParticipantRole.ORGANIZER, 1, "Organizer");
      assertEnumValue(CrmParticipantRole.ATTENDEE, 2, "Attendee");
      assertEnumValue(CrmParticipantRole.OPTIONAL, 3, "Optional");

      for(CrmParticipantRole value : CrmParticipantRole.values())
      {
         assertThat(CrmParticipantRole.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmSequenceStepType
    *******************************************************************************/
   @Test
   void testCrmSequenceStepType()
   {
      assertThat(CrmSequenceStepType.NAME).isNotNull();
      assertThat(CrmSequenceStepType.getById(null)).isNull();
      assertThat(CrmSequenceStepType.getById(999)).isNull();

      assertEnumValue(CrmSequenceStepType.EMAIL, 1, "Email");
      assertEnumValue(CrmSequenceStepType.CALL, 2, "Call");
      assertEnumValue(CrmSequenceStepType.TASK, 3, "Task");
      assertEnumValue(CrmSequenceStepType.WAIT, 4, "Wait");

      for(CrmSequenceStepType value : CrmSequenceStepType.values())
      {
         assertThat(CrmSequenceStepType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmEnrollmentStatus
    *******************************************************************************/
   @Test
   void testCrmEnrollmentStatus()
   {
      assertThat(CrmEnrollmentStatus.NAME).isNotNull();
      assertThat(CrmEnrollmentStatus.getById(null)).isNull();
      assertThat(CrmEnrollmentStatus.getById(999)).isNull();

      assertEnumValue(CrmEnrollmentStatus.ACTIVE, 1, "Active");
      assertEnumValue(CrmEnrollmentStatus.PAUSED, 2, "Paused");
      assertEnumValue(CrmEnrollmentStatus.COMPLETED, 3, "Completed");
      assertEnumValue(CrmEnrollmentStatus.BOUNCED, 4, "Bounced");
      assertEnumValue(CrmEnrollmentStatus.REPLIED, 5, "Replied");
      assertEnumValue(CrmEnrollmentStatus.UNENROLLED, 6, "Unenrolled");
      assertEnumValue(CrmEnrollmentStatus.FAILED, 7, "Failed");

      for(CrmEnrollmentStatus value : CrmEnrollmentStatus.values())
      {
         assertThat(CrmEnrollmentStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmEmailStatus
    *******************************************************************************/
   @Test
   void testCrmEmailStatus()
   {
      assertThat(CrmEmailStatus.NAME).isNotNull();
      assertThat(CrmEmailStatus.getById(null)).isNull();
      assertThat(CrmEmailStatus.getById(999)).isNull();

      assertEnumValue(CrmEmailStatus.DRAFT, 1, "Draft");
      assertEnumValue(CrmEmailStatus.SENT, 2, "Sent");
      assertEnumValue(CrmEmailStatus.DELIVERED, 3, "Delivered");
      assertEnumValue(CrmEmailStatus.FAILED, 4, "Failed");

      for(CrmEmailStatus value : CrmEmailStatus.values())
      {
         assertThat(CrmEmailStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmScorableEntity
    *******************************************************************************/
   @Test
   void testCrmScorableEntity()
   {
      assertThat(CrmScorableEntity.NAME).isNotNull();
      assertThat(CrmScorableEntity.getById(null)).isNull();
      assertThat(CrmScorableEntity.getById(999)).isNull();

      assertEnumValue(CrmScorableEntity.CONTACT, 1, "Contact");

      for(CrmScorableEntity value : CrmScorableEntity.values())
      {
         assertThat(CrmScorableEntity.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmScoreOperator
    *******************************************************************************/
   @Test
   void testCrmScoreOperator()
   {
      assertThat(CrmScoreOperator.NAME).isNotNull();
      assertThat(CrmScoreOperator.getById(null)).isNull();
      assertThat(CrmScoreOperator.getById(999)).isNull();

      assertEnumValue(CrmScoreOperator.EQUALS, 1, "Equals");
      assertEnumValue(CrmScoreOperator.NOT_EQUALS, 2, "Not Equals");
      assertEnumValue(CrmScoreOperator.CONTAINS, 3, "Contains");
      assertEnumValue(CrmScoreOperator.GT, 4, "Greater Than");
      assertEnumValue(CrmScoreOperator.LT, 5, "Less Than");
      assertEnumValue(CrmScoreOperator.IS_SET, 6, "Is Set");
      assertEnumValue(CrmScoreOperator.IS_NOT_SET, 7, "Is Not Set");
      assertEnumValue(CrmScoreOperator.DAYS_AGO_GT, 8, "Days Ago Greater Than");
      assertEnumValue(CrmScoreOperator.DAYS_AGO_LT, 9, "Days Ago Less Than");

      for(CrmScoreOperator value : CrmScoreOperator.values())
      {
         assertThat(CrmScoreOperator.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmAssignableEntity
    *******************************************************************************/
   @Test
   void testCrmAssignableEntity()
   {
      assertThat(CrmAssignableEntity.NAME).isNotNull();
      assertThat(CrmAssignableEntity.getById(null)).isNull();
      assertThat(CrmAssignableEntity.getById(999)).isNull();

      assertEnumValue(CrmAssignableEntity.CONTACT, 1, "Contact");
      assertEnumValue(CrmAssignableEntity.DEAL, 2, "Deal");

      for(CrmAssignableEntity value : CrmAssignableEntity.values())
      {
         assertThat(CrmAssignableEntity.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmAssignStrategy
    *******************************************************************************/
   @Test
   void testCrmAssignStrategy()
   {
      assertThat(CrmAssignStrategy.NAME).isNotNull();
      assertThat(CrmAssignStrategy.getById(null)).isNull();
      assertThat(CrmAssignStrategy.getById(999)).isNull();

      assertEnumValue(CrmAssignStrategy.SPECIFIC_USER, 1, "Specific User");
      assertEnumValue(CrmAssignStrategy.ROUND_ROBIN, 2, "Round Robin");
      assertEnumValue(CrmAssignStrategy.LEAST_ACTIVE, 3, "Least Active");

      for(CrmAssignStrategy value : CrmAssignStrategy.values())
      {
         assertThat(CrmAssignStrategy.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmConsentType
    *******************************************************************************/
   @Test
   void testCrmConsentType()
   {
      assertThat(CrmConsentType.NAME).isNotNull();
      assertThat(CrmConsentType.getById(null)).isNull();
      assertThat(CrmConsentType.getById(999)).isNull();

      assertEnumValue(CrmConsentType.EMAIL, 1, "Email");
      assertEnumValue(CrmConsentType.CALL, 2, "Call");
      assertEnumValue(CrmConsentType.SMS, 3, "SMS");
      assertEnumValue(CrmConsentType.DATA_PROCESSING, 4, "Data Processing");

      for(CrmConsentType value : CrmConsentType.values())
      {
         assertThat(CrmConsentType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmConsentStatus
    *******************************************************************************/
   @Test
   void testCrmConsentStatus()
   {
      assertThat(CrmConsentStatus.NAME).isNotNull();
      assertThat(CrmConsentStatus.getById(null)).isNull();
      assertThat(CrmConsentStatus.getById(999)).isNull();

      assertEnumValue(CrmConsentStatus.GRANTED, 1, "Granted");
      assertEnumValue(CrmConsentStatus.WITHDRAWN, 2, "Withdrawn");

      for(CrmConsentStatus value : CrmConsentStatus.values())
      {
         assertThat(CrmConsentStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmLegalBasis
    *******************************************************************************/
   @Test
   void testCrmLegalBasis()
   {
      assertThat(CrmLegalBasis.NAME).isNotNull();
      assertThat(CrmLegalBasis.getById(null)).isNull();
      assertThat(CrmLegalBasis.getById(999)).isNull();

      assertEnumValue(CrmLegalBasis.CONSENT, 1, "Consent");
      assertEnumValue(CrmLegalBasis.LEGITIMATE_INTEREST, 2, "Legitimate Interest");
      assertEnumValue(CrmLegalBasis.CONTRACT, 3, "Contract");
      assertEnumValue(CrmLegalBasis.LEGAL_OBLIGATION, 4, "Legal Obligation");

      for(CrmLegalBasis value : CrmLegalBasis.values())
      {
         assertThat(CrmLegalBasis.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmGoalPeriod
    *******************************************************************************/
   @Test
   void testCrmGoalPeriod()
   {
      assertThat(CrmGoalPeriod.NAME).isNotNull();
      assertThat(CrmGoalPeriod.getById(null)).isNull();
      assertThat(CrmGoalPeriod.getById(999)).isNull();

      assertEnumValue(CrmGoalPeriod.MONTHLY, 1, "Monthly");
      assertEnumValue(CrmGoalPeriod.QUARTERLY, 2, "Quarterly");
      assertEnumValue(CrmGoalPeriod.ANNUAL, 3, "Annual");

      for(CrmGoalPeriod value : CrmGoalPeriod.values())
      {
         assertThat(CrmGoalPeriod.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmSyncStatus
    *******************************************************************************/
   @Test
   void testCrmSyncStatus()
   {
      assertThat(CrmSyncStatus.NAME).isNotNull();
      assertThat(CrmSyncStatus.getById(null)).isNull();
      assertThat(CrmSyncStatus.getById(999)).isNull();

      assertEnumValue(CrmSyncStatus.SYNCED, 1, "Synced");
      assertEnumValue(CrmSyncStatus.PENDING, 2, "Pending");
      assertEnumValue(CrmSyncStatus.ERROR, 3, "Error");

      for(CrmSyncStatus value : CrmSyncStatus.values())
      {
         assertThat(CrmSyncStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmAuditAction
    *******************************************************************************/
   @Test
   void testCrmAuditAction()
   {
      assertThat(CrmAuditAction.NAME).isNotNull();
      assertThat(CrmAuditAction.getById(null)).isNull();
      assertThat(CrmAuditAction.getById(999)).isNull();

      assertEnumValue(CrmAuditAction.CREATED, 1, "Created");
      assertEnumValue(CrmAuditAction.UPDATED, 2, "Updated");
      assertEnumValue(CrmAuditAction.DELETED, 3, "Deleted");
      assertEnumValue(CrmAuditAction.STAGE_CHANGED, 4, "Stage Changed");
      assertEnumValue(CrmAuditAction.OWNER_CHANGED, 5, "Owner Changed");
      assertEnumValue(CrmAuditAction.SCORE_CHANGED, 6, "Score Changed");
      assertEnumValue(CrmAuditAction.MERGED, 7, "Merged");
      assertEnumValue(CrmAuditAction.CONSENT_CHANGED, 8, "Consent Changed");
      assertEnumValue(CrmAuditAction.TAG_CHANGED, 9, "Tag Changed");
      assertEnumValue(CrmAuditAction.ENROLLED, 10, "Enrolled");
      assertEnumValue(CrmAuditAction.UNENROLLED, 11, "Unenrolled");
      assertEnumValue(CrmAuditAction.IMPORTED, 12, "Imported");
      assertEnumValue(CrmAuditAction.ANONYMIZED, 13, "Anonymized");
      assertEnumValue(CrmAuditAction.FORM_CONVERTED, 14, "Form Converted");

      for(CrmAuditAction value : CrmAuditAction.values())
      {
         assertThat(CrmAuditAction.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmFormType
    *******************************************************************************/
   @Test
   void testCrmFormType()
   {
      assertThat(CrmFormType.NAME).isNotNull();
      assertThat(CrmFormType.getById(null)).isNull();
      assertThat(CrmFormType.getById(999)).isNull();

      assertEnumValue(CrmFormType.CONTACT, 1, "Contact");
      assertEnumValue(CrmFormType.DEMO_REQUEST, 2, "Demo Request");
      assertEnumValue(CrmFormType.NEWSLETTER, 3, "Newsletter");
      assertEnumValue(CrmFormType.NOTIFY_ME, 4, "Notify Me");
      assertEnumValue(CrmFormType.GENERAL, 5, "General");

      for(CrmFormType value : CrmFormType.values())
      {
         assertThat(CrmFormType.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmFormConversionStatus
    *******************************************************************************/
   @Test
   void testCrmFormConversionStatus()
   {
      assertThat(CrmFormConversionStatus.NAME).isNotNull();
      assertThat(CrmFormConversionStatus.getById(null)).isNull();
      assertThat(CrmFormConversionStatus.getById(999)).isNull();

      assertEnumValue(CrmFormConversionStatus.NEW, 1, "New");
      assertEnumValue(CrmFormConversionStatus.CONVERTED_TO_CONTACT, 2, "Converted to Contact");
      assertEnumValue(CrmFormConversionStatus.SPAM, 3, "Spam");
      assertEnumValue(CrmFormConversionStatus.IGNORED, 4, "Ignored");

      for(CrmFormConversionStatus value : CrmFormConversionStatus.values())
      {
         assertThat(CrmFormConversionStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Test CrmListMemberStatus
    *******************************************************************************/
   @Test
   void testCrmListMemberStatus()
   {
      assertThat(CrmListMemberStatus.NAME).isNotNull();
      assertThat(CrmListMemberStatus.getById(null)).isNull();
      assertThat(CrmListMemberStatus.getById(999)).isNull();

      assertEnumValue(CrmListMemberStatus.SUBSCRIBED, 1, "Subscribed");
      assertEnumValue(CrmListMemberStatus.UNSUBSCRIBED, 2, "Unsubscribed");
      assertEnumValue(CrmListMemberStatus.BOUNCED, 3, "Bounced");
      assertEnumValue(CrmListMemberStatus.CLEANED, 4, "Cleaned");

      for(CrmListMemberStatus value : CrmListMemberStatus.values())
      {
         assertThat(CrmListMemberStatus.getById(value.getId())).isEqualTo(value);
      }
   }



   /*******************************************************************************
    ** Helper to assert an enum value's id, label, and PossibleValueEnum contract.
    *******************************************************************************/
   private <T extends PossibleValueEnum<Integer>> void assertEnumValue(T enumValue, Integer expectedId, String expectedLabel)
   {
      assertThat(enumValue.getPossibleValueId()).isEqualTo(expectedId);
      assertThat(enumValue.getPossibleValueLabel()).isEqualTo(expectedLabel);
   }
}
