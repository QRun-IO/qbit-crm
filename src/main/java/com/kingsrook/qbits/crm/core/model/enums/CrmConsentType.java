/*******************************************************************************
 ** Possible value source enum for CRM consent types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM consent types (Email, Call, SMS, Data Processing).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmConsentType implements PossibleValueEnum<Integer>
{
   EMAIL(1, "Email"),
   CALL(2, "Call"),
   SMS(3, "SMS"),
   DATA_PROCESSING(4, "Data Processing");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmConsentType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmConsentType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmConsentType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmConsentType value : CrmConsentType.values())
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
