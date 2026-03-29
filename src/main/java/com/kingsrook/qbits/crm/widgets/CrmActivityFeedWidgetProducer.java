/*******************************************************************************
 ** MetaData producer for the CRM Activity Feed widget.
 **
 ** Provides a TABLE view showing the most recent 50 activities across the CRM
 ** with columns: subject, activityType, contact, company, date.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmActivityFeed widget (TABLE type).
 *******************************************************************************/
public class CrmActivityFeedWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmActivityFeed";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.TABLE.getType())
         .withGridColumns(6)
         .withIsCard(true)
         .withLabel("Activity Feed")
         .withTooltip("Recent CRM activities")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("event_note"))
         .withCodeReference(new QCodeReference(CrmActivityFeedRenderer.class));
   }

}
