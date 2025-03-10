/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;

import java.util.Arrays;
import java.util.Collection;
import org.apache.activemq.artemis.utils.ClassloadingUtil;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends PersistentResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTES = {
            CommonAttributes.FACTORY_CLASS,
            CommonAttributes.PARAMS };

    ConnectorServiceDefinition() {
        super(MessagingExtension.CONNECTOR_SERVICE_PATH,
                MessagingExtension.getResourceDescriptionResolver(false, CommonAttributes.CONNECTOR_SERVICE),
                new ConnectorServiceAddHandler(ATTRIBUTES),
                new ActiveMQReloadRequiredHandlers.RemoveStepHandler());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler writeHandler = new ConnectorServiceWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, writeHandler);
            }
        }
    }

    private static void checkFactoryClass(final String factoryClass) throws OperationFailedException {
        try {
            ClassloadingUtil.newInstanceFromClassLoader(factoryClass);
        } catch (Throwable t) {
            throw MessagingLogger.ROOT_LOGGER.unableToLoadConnectorServiceFactoryClass(factoryClass);
        }
    }

    static class ConnectorServiceWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        ConnectorServiceWriteAttributeHandler(AttributeDefinition... attributes) {
            super(attributes);
        }
        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                HandbackHolder<Void> voidHandback)
                throws OperationFailedException {
            if (CommonAttributes.FACTORY_CLASS.getName().equals(attributeName)) {
                checkFactoryClass(resolvedValue.asString());
            }
            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }
    }

    static class ConnectorServiceAddHandler extends ActiveMQReloadRequiredHandlers.AddStepHandler {
        ConnectorServiceAddHandler(AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
            checkFactoryClass(factoryClass);
            super.performRuntime(context, operation, model);
        }

    }
}