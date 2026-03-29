/*******************************************************************************
 ** Possible value source enum for CRM sync statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM sync statuses (Synced, Pending, Error).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmSyncStatus implements PossibleValueEnum<Integer>
{
   SYNCED(1, "Synced"),
   PENDING(2, "Pending"),
   ERROR(3, "Error");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmSyncStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmSyncStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmSyncStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmSyncStatus value : CrmSyncStatus.values())
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
