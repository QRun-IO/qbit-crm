/*******************************************************************************
 ** Possible value source enum for CRM sequence enrollment statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM sequence enrollment statuses.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmEnrollmentStatus implements PossibleValueEnum<Integer>
{
   ACTIVE(1, "Active"),
   PAUSED(2, "Paused"),
   COMPLETED(3, "Completed"),
   BOUNCED(4, "Bounced"),
   REPLIED(5, "Replied"),
   UNENROLLED(6, "Unenrolled"),
   FAILED(7, "Failed");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmEnrollmentStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmEnrollmentStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmEnrollmentStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmEnrollmentStatus value : CrmEnrollmentStatus.values())
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
