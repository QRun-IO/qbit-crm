/*******************************************************************************
 ** Possible value source enum for CRM scoring rule operators.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM scoring rule operators used in score rule conditions.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmScoreOperator implements PossibleValueEnum<Integer>
{
   EQUALS(1, "Equals"),
   NOT_EQUALS(2, "Not Equals"),
   CONTAINS(3, "Contains"),
   GT(4, "Greater Than"),
   LT(5, "Less Than"),
   IS_SET(6, "Is Set"),
   IS_NOT_SET(7, "Is Not Set"),
   DAYS_AGO_GT(8, "Days Ago Greater Than"),
   DAYS_AGO_LT(9, "Days Ago Less Than");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmScoreOperator";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmScoreOperator(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmScoreOperator getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmScoreOperator value : CrmScoreOperator.values())
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
