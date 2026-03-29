/*******************************************************************************
 ** Possible value source enum for CRM entity types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM entity types (Contact, Company, Deal, Activity).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmEntityType implements PossibleValueEnum<Integer>
{
   CONTACT(1, "Contact"),
   COMPANY(2, "Company"),
   DEAL(3, "Deal"),
   ACTIVITY(4, "Activity");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmEntityType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmEntityType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmEntityType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmEntityType value : CrmEntityType.values())
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
