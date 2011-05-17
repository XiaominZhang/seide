package net.sf.seide.event;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.seide.core.Dispatcher;
import net.sf.seide.core.RuntimeStage;
import net.sf.seide.message.JoinHandler;
import net.sf.seide.message.Message;
import net.sf.seide.stages.RoutingOutcome;
import net.sf.seide.stages.StageStatistics;

public class RunnableEventHandlerWrapper
    implements Runnable {

    private final Dispatcher dispatcher;
    private final RuntimeStage runtimeStage;
    private final Message message;
    private final StageStatistics stageStats;
    private final StageStatistics routingStageStats;
    @SuppressWarnings("rawtypes")
    private final EventHandler eventHandler;
    private final Event event;

    public RunnableEventHandlerWrapper(Dispatcher dispatcher, RuntimeStage runtimeStage, Event event) {
        this.dispatcher = dispatcher;
        this.runtimeStage = runtimeStage;
        this.message = event.getMessage();
        this.stageStats = runtimeStage.getStageStats();
        this.routingStageStats = runtimeStage.getRoutingStageStats();
        this.eventHandler = runtimeStage.getEventHandler();
        this.event = event;

        // asume that a creating means a pending...
        this.stageStats.addPending();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        // no longer pending...
        this.stageStats.removePending();
        // now running...
        this.stageStats.addRunning();

        long time = 0;
        time = System.nanoTime();

        Message returnMessage = null;
        try {
            // execution
            RoutingOutcome routingOutcome = this.eventHandler.execute(this.message);

            // tracking & remove running...
            this.stageStats.trackTimeAndExecution(System.nanoTime() - time);
            this.stageStats.removeRunning();

            // now track the routing times...
            this.routingStageStats.addRunning();
            time = System.nanoTime();
            if (routingOutcome != null) {
                // get the returnMessage out of the outcome...
                returnMessage = routingOutcome.getReturnMessage();

                // extract the events an fire them up!
                if (!routingOutcome.isEmpty()) {
                    Collection<Event> outcomeEvents = routingOutcome.getEvents();
                    if (routingOutcome.hasJoinEvent()) {
                        // configures an outcomeEvents list and attaches it to a JoinHandler firing the joinEvent.
                        outcomeEvents = this.createJoinEventAttachedOf(outcomeEvents, routingOutcome.getJoinEvent());
                    }

                    // real firing here...
                    this.fireEvents(outcomeEvents);
                }
            }
        } finally {
            this.routingStageStats.trackTimeAndExecution(System.nanoTime() - time);
            this.routingStageStats.removeRunning();

            JoinHandler joinHandler = this.event.getJoinHandler();
            if (joinHandler != null) {
                joinHandler.notifyChildOutcome(this.getRuntimeStage().getId(), returnMessage);
            }
        }
    }

    private Collection<Event> createJoinEventAttachedOf(Collection<Event> outcomeEvents, Event targetEvent) {
        Collection<Event> joinEventWrappedCollection = new ArrayList<Event>(outcomeEvents.size());
        JoinHandler joinHandler = new JoinHandler(this.dispatcher, targetEvent);
        for (Event event : outcomeEvents) {
            // create a JoinEvent wrapping the JoinHandler...
            joinEventWrappedCollection.add(new Event(event, joinHandler));

            // register child, if not registered it could leak a join thread
            joinHandler.registerChild(this.eventHandler);
        }
        return joinEventWrappedCollection;
    }

    private void fireEvents(Collection<Event> outcomeEvents) {
        for (Event e : outcomeEvents) {
            this.dispatcher.execute(e);
        }
    }

    public RuntimeStage getRuntimeStage() {
        return this.runtimeStage;
    }

}
