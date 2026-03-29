/*******************************************************************************
 ** MetaData producer for the CRM Contact Timeline widget.
 **
 ** Provides a STEPPER view showing a chronological timeline of activities for
 ** a specific contact. The contactId is passed as a widget input parameter.
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
 ** Produces metadata for the crmContactTimeline widget (STEPPER type).
 *******************************************************************************/
public class CrmContactTimelineWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmContactTimeline";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.STEPPER.getType())
         .withGridColumns(12)
         .withIsCard(true)
         .withLabel("Contact Timeline")
         .withTooltip("Chronological activity timeline for this contact")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new QIcon("timeline"))
         .withCodeReference(new QCodeReference(CrmContactTimelineRenderer.class));
   }

}
