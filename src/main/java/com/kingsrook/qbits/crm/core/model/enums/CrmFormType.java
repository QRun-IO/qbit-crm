/*******************************************************************************
 ** Possible value source enum for CRM form types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM form types (Contact, Demo Request, Newsletter, Notify Me, General).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmFormType implements PossibleValueEnum<Integer>
{
   CONTACT(1, "Contact"),
   DEMO_REQUEST(2, "Demo Request"),
   NEWSLETTER(3, "Newsletter"),
   NOTIFY_ME(4, "Notify Me"),
   GENERAL(5, "General");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmFormType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmFormType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmFormType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmFormType value : CrmFormType.values())
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
