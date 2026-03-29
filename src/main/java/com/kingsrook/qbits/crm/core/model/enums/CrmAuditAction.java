/*******************************************************************************
 ** Possible value source enum for CRM audit log actions.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM audit log actions tracked for entity changes.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmAuditAction implements PossibleValueEnum<Integer>
{
   CREATED(1, "Created"),
   UPDATED(2, "Updated"),
   DELETED(3, "Deleted"),
   STAGE_CHANGED(4, "Stage Changed"),
   OWNER_CHANGED(5, "Owner Changed"),
   SCORE_CHANGED(6, "Score Changed"),
   MERGED(7, "Merged"),
   CONSENT_CHANGED(8, "Consent Changed"),
   TAG_CHANGED(9, "Tag Changed"),
   ENROLLED(10, "Enrolled"),
   UNENROLLED(11, "Unenrolled"),
   IMPORTED(12, "Imported"),
   ANONYMIZED(13, "Anonymized"),
   FORM_CONVERTED(14, "Form Converted");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmAuditAction";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmAuditAction(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmAuditAction getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmAuditAction value : CrmAuditAction.values())
      {
         if(Objects.equals(value.id, id))
         {
            return (value);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {
      return (getId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (getLabel());
   }
}
