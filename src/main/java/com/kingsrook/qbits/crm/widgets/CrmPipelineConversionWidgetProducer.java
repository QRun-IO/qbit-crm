/*******************************************************************************
 ** MetaData producer for the Pipeline Conversion widget.
 **
 ** Shows conversion rates between pipeline stages based on DealStageHistory
 ** transition data.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmPipelineConversion widget (TABLE type).
 *******************************************************************************/
public class CrmPipelineConversionWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmPipelineConversion";



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
         .withLabel("Pipeline Conversion")
         .withTooltip("Stage transition conversion rates")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("funnel_chart"))
         .withCodeReference(new QCodeReference(CrmPipelineConversionRenderer.class));
   }

}
