/*
 * Copyright 2016, Emanuel Rabina (http://www.ultraq.net.nz/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.net.ultraq.thymeleaf.decorators.html;

import java.util.LinkedHashMap;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.ModelBuilder;
import nz.net.ultraq.thymeleaf.models.ElementMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IText;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardTextTagProcessor;
import org.thymeleaf.util.StringUtils;
import org.unbescape.html.HtmlEscape;

/**
 * Decorator for the {@code <title>} part of the template to handle the special
 * processing required for the {@code layout:title-pattern} processor.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class HtmlTitleDecorator implements Decorator {

    // Get the title pattern to use
    private static IAttribute titlePatternProcessorRetriever(IModel titleModel) {
        if (!MetaClass.asBoolean(titleModel)) {
            return null;
        }
        IProcessableElementTag event = (IProcessableElementTag) MetaClass.first(titleModel);
        return event != null ? event.getAttribute(LayoutDialect.DIALECT_PREFIX, TitlePatternProcessor.PROCESSOR_NAME) : null;
    }

    private static String titleValueRetriever(IModel titleModel) {
        if (MetaClass.asBoolean(titleModel)) {
            IProcessableElementTag event = (IProcessableElementTag) MetaClass.first(titleModel);
            String result = null;
            if (event != null) {
                result = event.getAttributeValue(StandardDialect.PREFIX, StandardTextTagProcessor.ATTR_NAME);
            }
            if (!StringUtils.isEmpty(result)) {
                return result;
            }
            if (titleModel.size() > 2) {
                return "'" + HtmlEscape.escapeHtml5Xml(((IText) titleModel.get(1)).getText()) + "'";
            }
        }
        return null;
    }

    private final ITemplateContext context;

    /**
     * Constructor, sets up the decorator context.
     *
     * @param context
     */
    public HtmlTitleDecorator(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Special decorator for the {@code <title>} part, accumulates the important
     * processing parts for the {@code layout:title-pattern} processor.
     *
     * @param targetTitleModel
     * @param sourceTitleModel
     * @return A new {@code <title>} model that is the result of decorating the
     * {@code <title>}.
     */
    @Override
    public IModel decorate(IModel targetTitleModel, IModel sourceTitleModel) {
        IAttribute titlePatternProcessor = titlePatternProcessorRetriever(sourceTitleModel);
        if (titlePatternProcessor == null) {
            titlePatternProcessor = titlePatternProcessorRetriever(targetTitleModel);
        }

        IModel resultTitle;

        // Set the title pattern to use on a new model, as well as the important
        // title result parts that we want to use on the pattern.
        if (titlePatternProcessor != null) {
            LinkedHashMap<String, Object> titleValuesMap = new LinkedHashMap<>(2);
            String contentTitle = titleValueRetriever(sourceTitleModel);
            if (!StringUtils.isEmpty(contentTitle)) {
                titleValuesMap.put(TitlePatternProcessor.CONTENT_TITLE_ATTRIBUTE, contentTitle);
            }
            String layoutTitle = titleValueRetriever(targetTitleModel);
            if (!StringUtils.isEmpty(layoutTitle)) {
                titleValuesMap.put(TitlePatternProcessor.LAYOUT_TITLE_ATTRIBUTE, layoutTitle);
            }
            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>(3);
            linkedHashMap.put(titlePatternProcessor.getAttributeCompleteName(), titlePatternProcessor.getValue());
            linkedHashMap.putAll(titleValuesMap);
            resultTitle = new ModelBuilder(context).createNode("title", linkedHashMap);
        } else {
            resultTitle = new ElementMerger(context.getModelFactory()).merge(targetTitleModel, sourceTitleModel);
        }
        return resultTitle;
    }

}
