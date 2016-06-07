/*
 * Copyright 2015, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.models;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.fragments.FragmentProcessor;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardWithTagProcessor;

/**
 * Merges attributes from one element into another.
 *
 * @author Emanuel Rabina
 */
public class AttributeMerger implements ModelMerger {

    private final IModelFactory modelFactory;

    /**
     * Constructor, sets up the attribute merger tools.
     *
     * @param modelFactory
     */
    public AttributeMerger(IModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    /**
     * Merge the attributes of the source element with those of the target
     * element. This is basically a copy of all attributes in the source model
     * with those in the target model, overwriting any attributes that have the
     * same name, except for the case of {@code th:with} where variable
     * declarations are preserved, only overwriting same-named declarations.
     *
     * @param sourceModel
     * @param targetModel
     */
    @Override
    public void merge(IModel targetModel, IModel sourceModel) {
        if (!MetaClass.asBoolean(targetModel) || !MetaClass.asBoolean(sourceModel)) {
            return;
        }

        // Merge attributes from the source model's root event to the target model's root event
        for (IAttribute sourceAttribute : ((IProcessableElementTag) sourceModel.get(0)).getAllAttributes()) {
            // Don't include layout:fragment processors
            if (MetaClass.equalsName(sourceAttribute, LayoutDialect.DIALECT_PREFIX, FragmentProcessor.PROCESSOR_NAME)) {
                continue;
            }
            IProcessableElementTag targetEvent = (IProcessableElementTag) targetModel.get(0);
            String mergedAttributeValue;

            // Merge th:with attributes
            if (MetaClass.equalsName(sourceAttribute, StandardDialect.PREFIX, StandardWithTagProcessor.ATTR_NAME)) {
                mergedAttributeValue = new VariableDeclarationMerger().merge(sourceAttribute.getValue(),
                        targetEvent.getAttributeValue(StandardDialect.PREFIX, StandardWithTagProcessor.ATTR_NAME));
            } else { // Copy every other attribute straight
                mergedAttributeValue = sourceAttribute.getValue();
            }

            targetModel.replace(0, modelFactory.replaceAttribute(targetEvent,
                    MetaClass.getAttributeName(sourceAttribute), sourceAttribute.getAttributeCompleteName(),
                    mergedAttributeValue));
        }
    }

}
