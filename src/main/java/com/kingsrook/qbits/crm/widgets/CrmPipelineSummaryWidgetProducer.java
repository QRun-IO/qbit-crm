/*******************************************************************************
 ** MetaData producer for the CRM Pipeline Summary widget.
 **
 ** Provides a TABLE view showing deal counts and amounts grouped by pipeline
 ** stage for a given pipeline.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmPipelineSummary widget (TABLE type).
 *******************************************************************************/
public class CrmPipelineSummaryWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmPipelineSummary";



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
         .withLabel("Pipeline Summary")
         .withTooltip("Deal counts and amounts by pipeline stage")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("linear_scale"))
         .withCodeReference(new QCodeReference(CrmPipelineSummaryRenderer.class));
   }

}
