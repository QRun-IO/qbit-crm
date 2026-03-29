/*******************************************************************************
 ** MetaData producer for the CRM Activity Count widget.
 **
 ** Provides a MULTI_STATISTICS view showing counts of activities by type for
 ** the current user today: calls, emails, meetings.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** Produces metadata for the crmActivityCount widget (MULTI_STATISTICS type).
 *******************************************************************************/
public class CrmActivityCountWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmActivityCount";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.MULTI_STATISTICS.getType())
         .withGridColumns(6)
         .withIsCard(true)
         .withLabel("Activity Counts (Today)")
         .withTooltip("Activity counts by type for today")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new QIcon("analytics"))
         .withCodeReference(new QCodeReference(CrmActivityCountRenderer.class));
   }

}
