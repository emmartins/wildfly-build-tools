package org.wildfly.build.forge;

import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public class FeaturePackAddCommand extends AbstractUICommand implements UICommand {

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    @WithAttributes(label="Feature Pack's artifact", required=true)
    private UIInput<String> artifact;

    @Override
    public Metadata getMetadata(UIContext context) {
        return Metadata
                .from(super.getMetadata(context), getClass())
                .name("add-feature-pack")
                .description("Add Feature Pack to WildFly Server Provisioning")
                .category(Categories.create("WildFly Build Tools"));
    }
    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(artifact);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = (ServerProvisioningDescriptionResource) context.getUIContext().getSelection().get();
        final ServerProvisioningDescription.FeaturePack featurePack = new ServerProvisioningDescription.FeaturePack(artifact.getValue());
        descriptionResource.getServerProvisioningDescription().getFeaturePacks().add(featurePack);
        descriptionResource.setContents();
        context.getUIContext().setSelection(new ServerProvisioningDescriptionFeaturePackResourceImpl(resourceFactory, descriptionResource, featurePack));
        return Results.success();
    }

    @Override
    public boolean isEnabled(UIContext context) {
        return super.isEnabled(context) && (context.getSelection().get() instanceof ServerProvisioningDescriptionResource);
    }
}
