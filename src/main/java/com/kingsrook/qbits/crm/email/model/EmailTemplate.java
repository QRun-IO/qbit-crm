/*******************************************************************************
 ** QRecord Entity for the crm_email_template table.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.email.customizers.EmailTemplatePreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** Reusable email template with merge-field support. Templates can be personal
 ** or shared across the team, and are referenced by email sequences and manual
 ** sends.
 *******************************************************************************/
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   produceTableMetaData = true,
   tableMetaDataCustomizer = EmailTemplate.TableMetaDataCustomizer.class
)
public class EmailTemplate extends QRecordEntity
{
   public static final String TABLE_NAME = "crmEmailTemplate";



   /***************************************************************************
    ** Customizer that sets icon, record label, section layout, and registers
    ** PRE_DELETE customizer to prevent deletion of actively-used templates.
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
            .withIcon(new QIcon().withName("description"))
            .withRecordLabelFormat("%s")
            .withRecordLabelFields("name")
            .withSection(SectionFactory.defaultT1("id", "name", "subject", "category"))
            .withSection(new QFieldSection("content", new QIcon("article"), Tier.T2,
               List.of("bodyHtml", "bodyText")))
            .withSection(new QFieldSection("ownership", new QIcon("person"), Tier.T2,
               List.of("ownerUserId", "isShared")))
            .withSection(SectionFactory.defaultT3("createDate", "modifyDate"));

         ////////////////////////////////////////////////////////////////////
         // register PRE_DELETE to block deletion of actively-used templates //
         ////////////////////////////////////////////////////////////////////
         table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(EmailTemplatePreDeleteCustomizer.class));

         return (table);
      }
   }



   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(isRequired = true, maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String subject;

   @QField(isRequired = true)
   private String bodyHtml;

   @QField()
   private String bodyText;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String category;

   @QField(isRequired = true)
   private String ownerUserId;

   @QField(isRequired = true, defaultValue = "false")
   private Boolean isShared;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public EmailTemplate()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public EmailTemplate(QRecord record)
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
   public EmailTemplate withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public EmailTemplate withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subject
    *******************************************************************************/
   public String getSubject()
   {
      return (this.subject);
   }



   /*******************************************************************************
    ** Setter for subject
    *******************************************************************************/
   public void setSubject(String subject)
   {
      this.subject = subject;
   }



   /*******************************************************************************
    ** Fluent setter for subject
    *******************************************************************************/
   public EmailTemplate withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyHtml
    *******************************************************************************/
   public String getBodyHtml()
   {
      return (this.bodyHtml);
   }



   /*******************************************************************************
    ** Setter for bodyHtml
    *******************************************************************************/
   public void setBodyHtml(String bodyHtml)
   {
      this.bodyHtml = bodyHtml;
   }



   /*******************************************************************************
    ** Fluent setter for bodyHtml
    *******************************************************************************/
   public EmailTemplate withBodyHtml(String bodyHtml)
   {
      this.bodyHtml = bodyHtml;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyText
    *******************************************************************************/
   public String getBodyText()
   {
      return (this.bodyText);
   }



   /*******************************************************************************
    ** Setter for bodyText
    *******************************************************************************/
   public void setBodyText(String bodyText)
   {
      this.bodyText = bodyText;
   }



   /*******************************************************************************
    ** Fluent setter for bodyText
    *******************************************************************************/
   public EmailTemplate withBodyText(String bodyText)
   {
      this.bodyText = bodyText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for category
    *******************************************************************************/
   public String getCategory()
   {
      return (this.category);
   }



   /*******************************************************************************
    ** Setter for category
    *******************************************************************************/
   public void setCategory(String category)
   {
      this.category = category;
   }



   /*******************************************************************************
    ** Fluent setter for category
    *******************************************************************************/
   public EmailTemplate withCategory(String category)
   {
      this.category = category;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ownerUserId
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (this.ownerUserId);
   }



   /*******************************************************************************
    ** Setter for ownerUserId
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** Fluent setter for ownerUserId
    *******************************************************************************/
   public EmailTemplate withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isShared
    *******************************************************************************/
   public Boolean getIsShared()
   {
      return (this.isShared);
   }



   /*******************************************************************************
    ** Setter for isShared
    *******************************************************************************/
   public void setIsShared(Boolean isShared)
   {
      this.isShared = isShared;
   }



   /*******************************************************************************
    ** Fluent setter for isShared
    *******************************************************************************/
   public EmailTemplate withIsShared(Boolean isShared)
   {
      this.isShared = isShared;
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
   public EmailTemplate withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public EmailTemplate withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
