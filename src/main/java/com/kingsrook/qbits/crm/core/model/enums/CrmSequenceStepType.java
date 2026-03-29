/*******************************************************************************
 ** Possible value source enum for CRM sequence step types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM sequence step types (Email, Call, Task, Wait).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmSequenceStepType implements PossibleValueEnum<Integer>
{
   EMAIL(1, "Email"),
   CALL(2, "Call"),
   TASK(3, "Task"),
   WAIT(4, "Wait");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmSequenceStepType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmSequenceStepType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmSequenceStepType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmSequenceStepType value : CrmSequenceStepType.values())
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
