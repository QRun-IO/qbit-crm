/*******************************************************************************
 ** Possible value source enum for CRM consent statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM consent statuses (Granted, Withdrawn).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmConsentStatus implements PossibleValueEnum<Integer>
{
   GRANTED(1, "Granted"),
   WITHDRAWN(2, "Withdrawn");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmConsentStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmConsentStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmConsentStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmConsentStatus value : CrmConsentStatus.values())
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
