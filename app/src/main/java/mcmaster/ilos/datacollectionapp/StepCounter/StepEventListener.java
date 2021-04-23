package mcmaster.ilos.datacollectionapp.StepCounter;

public interface StepEventListener {
    void onStepEvent(int totalSteps, long timestamp);
}
