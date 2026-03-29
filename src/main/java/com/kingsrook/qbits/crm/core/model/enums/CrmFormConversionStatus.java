/*******************************************************************************
 ** Possible value source enum for CRM form conversion statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM form submission conversion statuses.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmFormConversionStatus implements PossibleValueEnum<Integer>
{
   NEW(1, "New"),
   CONVERTED_TO_CONTACT(2, "Converted to Contact"),
   SPAM(3, "Spam"),
   IGNORED(4, "Ignored");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmFormConversionStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmFormConversionStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmFormConversionStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmFormConversionStatus value : CrmFormConversionStatus.values())
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
