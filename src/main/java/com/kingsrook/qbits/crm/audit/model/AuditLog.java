/*******************************************************************************
 ** QRecord Entity for the immutable CRM audit log table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.audit.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** Immutable append-only audit ledger for tracked CRM entities.
 ** Records field-level changes, stage transitions, owner changes, and lifecycle
 ** events with full before/after values and user attribution.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = false,
   produceTableMetaData = true,
   tableMetaDataCustomizer = AuditLog.TableMetaDataCustomizer.class
)
public class AuditLog extends QRecordEntity
{
   public static final String TABLE_NAME = "crmAuditLog";


   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = CrmEntityType.NAME)
   private Integer entityType;

   @QField(isRequired = true)
   private Integer entityId;

   @QField(isRequired = true, possibleValueSourceName = CrmAuditAction.NAME)
   private Integer action;

   @QField(isRequired = true)
   private String userId;

   @QField(maxLength = 100)
   private String fieldName;

   @QField()
   private String oldValue;

   @QField()
   private String newValue;

   @QField(maxLength = 1000)
   private String message;

   @QField(isEditable = false)
   private Instant createDate;



   /***************************************************************************
    ** TableMetaDataCustomizer -- sets icon, record label, sections, and
    ** registers PRE_UPDATE and PRE_DELETE customizers to enforce immutability.
    ***************************************************************************/
   public static class TableMetaDataCustomizer implements MetaDataCustomizerInterface<QTableMetaData>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QTableMetaData customizeMetaData(QInstance qInstance, QTableMetaData table) throws QException
      {
         table
            .withIcon(new QIcon().withName("history"))
            .withRecordLabelFormat("%s %s #%s")
            .withRecordLabelFields("action", "entityType", "entityId")
            .withSection(SectionFactory.defaultT1("id", "entityType", "entityId", "action"))
            .withSection(new QFieldSection("details", new QIcon("text_snippet"), Tier.T2,
               List.of("userId", "fieldName", "oldValue", "newValue", "message")))
            .withSection(SectionFactory.defaultT3("createDate"));

         ///////////////////////////////////////////////////////////////////////////
         // register PRE_UPDATE and PRE_DELETE customizers to enforce immutability //
         ///////////////////////////////////////////////////////////////////////////
         table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PreventUpdateCustomizer.class));
         table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(PreventDeleteCustomizer.class));

         return (table);
      }
   }



   /***************************************************************************
    ** PRE_UPDATE customizer that rejects all update attempts on audit log.
    ***************************************************************************/
   public static class PreventUpdateCustomizer extends AbstractPreUpdateCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         if("true".equals(QContext.getQSession().getValue("gdprBypass")))
         {
            return records;
         }
         throw new QUserFacingException("Audit log records cannot be modified or deleted");
      }
   }



   /***************************************************************************
    ** PRE_DELETE customizer that rejects all delete attempts on audit log.
    ***************************************************************************/
   public static class PreventDeleteCustomizer extends AbstractPreDeleteCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         if("true".equals(QContext.getQSession().getValue("gdprBypass")))
         {
            return records;
         }
         throw new QUserFacingException("Audit log records cannot be modified or deleted");
      }
   }



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public AuditLog()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public AuditLog(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public AuditLog withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entityType
    *******************************************************************************/
   public Integer getEntityType()
   {
      return (this.entityType);
   }



   /*******************************************************************************
    ** Setter for entityType
    *******************************************************************************/
   public void setEntityType(Integer entityType)
   {
      this.entityType = entityType;
   }



   /*******************************************************************************
    ** Fluent setter for entityType
    *******************************************************************************/
   public AuditLog withEntityType(Integer entityType)
   {
      this.entityType = entityType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entityId
    *******************************************************************************/
   public Integer getEntityId()
   {
      return (this.entityId);
   }



   /*******************************************************************************
    ** Setter for entityId
    *******************************************************************************/
   public void setEntityId(Integer entityId)
   {
      this.entityId = entityId;
   }



   /*******************************************************************************
    ** Fluent setter for entityId
    *******************************************************************************/
   public AuditLog withEntityId(Integer entityId)
   {
      this.entityId = entityId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for action
    *******************************************************************************/
   public Integer getAction()
   {
      return (this.action);
   }



   /*******************************************************************************
    ** Setter for action
    *******************************************************************************/
   public void setAction(Integer action)
   {
      this.action = action;
   }



   /*******************************************************************************
    ** Fluent setter for action
    *******************************************************************************/
   public AuditLog withAction(Integer action)
   {
      this.action = action;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public AuditLog withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public AuditLog withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for oldValue
    *******************************************************************************/
   public String getOldValue()
   {
      return (this.oldValue);
   }



   /*******************************************************************************
    ** Setter for oldValue
    *******************************************************************************/
   public void setOldValue(String oldValue)
   {
      this.oldValue = oldValue;
   }



   /*******************************************************************************
    ** Fluent setter for oldValue
    *******************************************************************************/
   public AuditLog withOldValue(String oldValue)
   {
      this.oldValue = oldValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for newValue
    *******************************************************************************/
   public String getNewValue()
   {
      return (this.newValue);
   }



   /*******************************************************************************
    ** Setter for newValue
    *******************************************************************************/
   public void setNewValue(String newValue)
   {
      this.newValue = newValue;
   }



   /*******************************************************************************
    ** Fluent setter for newValue
    *******************************************************************************/
   public AuditLog withNewValue(String newValue)
   {
      this.newValue = newValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public AuditLog withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public AuditLog withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }

}
