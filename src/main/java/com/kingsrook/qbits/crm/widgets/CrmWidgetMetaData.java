/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;


/*******************************************************************************
 ** QQQ 4.1.0-RC.6 widgets lack SourceQBitAware. Preserve their metadata while
 ** supplying the same provenance interface as tables and processes.
 ** Remove this adapter when QWidgetMetaData implements SourceQBitAware.
 *******************************************************************************/
public class CrmWidgetMetaData extends QWidgetMetaData implements SourceQBitAware
{
   private String sourceQBitName;



   /*******************************************************************************
    ** Copy the produced widget without losing renderer settings or validation.
    *******************************************************************************/
   public CrmWidgetMetaData(QWidgetMetaData widget)
   {
      setName(widget.getName());
      setIcon(widget.getIcon());
      setLabel(widget.getLabel());
      setTooltip(widget.getTooltip());
      setType(widget.getType());
      setMinHeight(widget.getMinHeight());
      setFooterHTML(widget.getFooterHTML());
      setIsCard(widget.getIsCard());
      setGridColumns(widget.getGridColumns());
      setCodeReference(widget.getCodeReference());
      setPermissionRules(widget.getPermissionRules());
      setDropdowns(widget.getDropdowns());
      setStoreDropdownSelections(widget.getStoreDropdownSelections());
      setShowReloadButton(widget.getShowReloadButton());
      setShowExportButton(widget.getShowExportButton());
      setIcons(widget.getIcons());
      setHelpContent(widget.getHelpContent());
      setDefaultValues(widget.getDefaultValues());
      setValidatorPlugin(widget.getValidatorPlugin());
      setCollapsible(widget.getCollapsible());
   }



   /*******************************************************************************
    ** Source QBit identifier.
    *******************************************************************************/
   @Override
   public String getSourceQBitName()
   {
      return (sourceQBitName);
   }



   /*******************************************************************************
    ** Set the source QBit identifier.
    *******************************************************************************/
   @Override
   public void setSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
   }



   /*******************************************************************************
    ** Fluent source QBit identifier.
    *******************************************************************************/
   @Override
   public CrmWidgetMetaData withSourceQBitName(String sourceQBitName)
   {
      setSourceQBitName(sourceQBitName);
      return (this);
   }

}
