/*******************************************************************************
 ** Possible value source enum for CRM deal win/loss types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM deal win/loss types (Win, Loss).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmWinLossType implements PossibleValueEnum<Integer>
{
   WIN(1, "Win"),
   LOSS(2, "Loss");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmWinLossType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmWinLossType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmWinLossType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmWinLossType value : CrmWinLossType.values())
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
