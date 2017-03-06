package de.marvinkerkhoff.form2db.setup;

import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.delta.BootstrapSingleResource;
import info.magnolia.module.delta.DeltaBuilder;

/**
 * This class is optional and lets you manager the versions of your module, by
 * registering "deltas" to maintain the module's configuration, or other type of
 * content. If you don't need this, simply remove the reference to this class in
 * the module descriptor xml.
 */
public class Form2dbVersionHandler extends DefaultModuleVersionHandler {

    public Form2dbVersionHandler() {

        register(DeltaBuilder.update("1.0.1", "")
                .addTask(new BootstrapSingleResource("Reinstall app config", "Bootstrap app config", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.apps.form2db.xml"))
        );

        register(DeltaBuilder.update("1.0.2", "")
                .addTask(new BootstrapSingleResource("Reinstall app config", "Bootstrap app config", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.apps.form2db.xml"))
        );
        
        register(DeltaBuilder.update("1.1.2", "")
                .addTask(new BootstrapSingleResource("Reinstall app config", "Bootstrap app config", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.apps.form2db.xml"))
        );
        
        register(DeltaBuilder.update("1.1.3", "")
                .addTask(new BootstrapSingleResource("Reinstall app config", "Bootstrap app config", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.apps.form2db.xml"))
        );
        
        register(DeltaBuilder.update("1.1.4", "")
                .addTask(new BootstrapSingleResource("Reinstall app config", "Bootstrap app config", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.apps.form2db.xml"))
                .addTask(new BootstrapSingleResource("Reinstall formProcessor", "Enable formProcessor by default", "/mgnl-bootstrap/form2db-app/config.modules.form.templates.components.form.formProcessors.form2db.xml"))
                .addTask(new BootstrapSingleResource("Add JCR tab to form dialog", "Add JCR tab to form dialog", "/mgnl-bootstrap/form2db-app/config.modules.form.dialogs.form.form.tabs.tabJcr.xml"))
        );

        register(DeltaBuilder.update("1.2.1", "")
            .addTask(new BootstrapSingleResource("Add Module Config in JCR", "Add Module Config in JCR", "/mgnl-bootstrap/form2db-app/config.modules.form2db-app.config.xml"))
        );

    }

}