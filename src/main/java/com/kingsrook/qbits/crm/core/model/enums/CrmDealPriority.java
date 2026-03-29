/*******************************************************************************
 ** Possible value source enum for CRM deal priority levels.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM deal priority levels (Low, Medium, High, Critical).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmDealPriority implements PossibleValueEnum<Integer>
{
   LOW(1, "Low"),
   MEDIUM(2, "Medium"),
   HIGH(3, "High"),
   CRITICAL(4, "Critical");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmDealPriority";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmDealPriority(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmDealPriority getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmDealPriority value : CrmDealPriority.values())
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
