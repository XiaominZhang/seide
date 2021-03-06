package net.sf.seide.controller;

import net.sf.seide.core.DispatcherAware;
import net.sf.seide.core.Lifecycle;
import net.sf.seide.core.RuntimeStage;
import net.sf.seide.event.Event;
import net.sf.seide.message.Message;
import net.sf.seide.stages.Stage;

/**
 * The {@link StageController} is the responsible for the execution of the given {@link Event}, in this case, only the
 * {@link Message} because there is an instance of this class per defined {@link Stage}.
 * 
 * @author german.kondolf
 */
public interface StageController
    extends DispatcherAware, Lifecycle {

    /**
     * Execution handler for the given stage.
     * 
     * @param event
     */
    void execute(Event event);

    /**
     * Runtime configuration of the running {@link Stage} ({@link RuntimeStage}).
     * 
     * @param runtimeStage
     */
    void setRuntimeStage(RuntimeStage runtimeStage);

}
