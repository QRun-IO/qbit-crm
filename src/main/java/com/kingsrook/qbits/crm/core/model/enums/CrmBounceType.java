/*******************************************************************************
 ** Possible value source enum for CRM email bounce types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM email bounce types (Hard, Soft).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmBounceType implements PossibleValueEnum<Integer>
{
   HARD(1, "Hard"),
   SOFT(2, "Soft");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmBounceType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmBounceType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmBounceType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmBounceType value : CrmBounceType.values())
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
