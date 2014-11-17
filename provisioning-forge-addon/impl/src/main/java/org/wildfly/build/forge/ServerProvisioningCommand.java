package org.wildfly.build.forge;

import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.forge.furnace.util.OperatingSystemUtils;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Eduardo Martins
 */
public class ServerProvisioningCommand extends AbstractUICommand implements UIWizard {

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    @WithAttributes(label="Server Provisioning XML fileName name", required=true, defaultValue = "server-provisioning.xml")
    private UIInput<String> fileName;

    @Override
    public Metadata getMetadata(UIContext context) {
        return Metadata
                .from(super.getMetadata(context), getClass())
                .name("server-provisioning")
                .description("WildFly Server Provisioning")
                .category(Categories.create("WildFly Build Tools"));
    }
    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(fileName);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        String fileName = this.fileName.getValue();
        ServerProvisioningDescriptionResource resource = resourceFactory.create(ServerProvisioningDescriptionResource.class, new File(OperatingSystemUtils.getWorkingDir(), fileName));
        if (!resource.exists()) {
            context.getUIContext().getAttributeMap().put("fileName", fileName);
            return Results.navigateTo(CreateServerProvisioningDescriptionStep.class);
        }
        context.getUIContext().setSelection(resource);
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    @Override
    public boolean isEnabled(UIContext context) {
        return super.isEnabled(context) && !(context.getSelection().get() instanceof ServerProvisioningDescriptionResource);
    }
}
