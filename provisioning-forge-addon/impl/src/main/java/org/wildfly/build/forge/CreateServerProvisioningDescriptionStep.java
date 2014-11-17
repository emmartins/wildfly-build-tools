package org.wildfly.build.forge;

import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIPrompt;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Eduardo Martins
 */
public class CreateServerProvisioningDescriptionStep extends AbstractUICommand implements UIWizardStep {

    @Inject
    private UIInput<Boolean> copyModuleArtifacts;

    @Inject
    private UIInput<Boolean> extractSchemas;

    @Inject
    private ResourceFactory resourceFactory;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(copyModuleArtifacts).add(extractSchemas);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        // End of interaction, return null
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final UIContext uiContext = context.getUIContext();
        String fileName = (String) uiContext.getAttributeMap().get("fileName");
        if (fileName == null) {
            fileName = "server-provisioning.xml";
        }
        final ServerProvisioningDescriptionResource resource = resourceFactory.create(ServerProvisioningDescriptionResource.class, new File(OperatingSystemUtils.getWorkingDir(), fileName));
        final ServerProvisioningDescription description = new ServerProvisioningDescription();
        final UIPrompt prompt = context.getPrompt();
        if (copyModuleArtifacts.hasValue()) {
            description.setCopyModuleArtifacts(copyModuleArtifacts.getValue());
        } else {
            description.setCopyModuleArtifacts(prompt.promptBoolean("Copy module artifacts?", true));
        }
        if (extractSchemas.hasValue()) {
            description.setExtractSchemas(extractSchemas.getValue());
        } else {
            description.setExtractSchemas(prompt.promptBoolean("Extract XML schemas from module artifacts?", false));
        }
        resource.setContents(description);
        uiContext.setSelection(resource);
        return Results.success(fileName + " created");
    }
}
