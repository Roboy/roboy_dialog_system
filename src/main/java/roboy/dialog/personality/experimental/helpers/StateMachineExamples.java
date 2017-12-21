package roboy.dialog.personality.experimental.helpers;

import roboy.dialog.personality.experimental.DialogStateMachine;
import roboy.dialog.personality.experimental.toyStates.ToyFarewellState;
import roboy.dialog.personality.experimental.toyStates.ToyGreetingsState;
import roboy.dialog.personality.experimental.toyStates.ToyIntroState;
import roboy.dialog.personality.experimental.toyStates.ToyRandomAnswerState;

import java.io.File;

/**
 * This class provides examples how to load state machines from files
 * or create them from code directly.
 */
public class StateMachineExamples {

    public static void main(String[] args) throws Exception {
        // create all states and set all connections from code directly
        fromCode();

        // load states and connections from file
        fromFile();
    }

    private static void fromCode() {

        ToyGreetingsState greetings = new ToyGreetingsState("Greetings");
        ToyIntroState intro = new ToyIntroState("Intro");
        ToyFarewellState farewell = new ToyFarewellState("Farewell");
        ToyRandomAnswerState randomAnswer = new ToyRandomAnswerState("RandomAnswer");

        greetings.setFallback(randomAnswer);
        greetings.setTransition("next", intro);
        greetings.setTransition("noHello", farewell);

        intro.setTransition("next", farewell);

        randomAnswer.setTransition("next", farewell);


        DialogStateMachine stateMachine = new DialogStateMachine();
        stateMachine.addState(greetings);
        stateMachine.addState(intro);
        stateMachine.addState(farewell);
        stateMachine.addState(randomAnswer);
        stateMachine.setActiveState(greetings);

        System.out.println(stateMachine);


    }

    private static void fromFile() throws Exception {

        DialogStateMachine stateMachine = new DialogStateMachine();
        stateMachine.loadStateMachine(new File("resources/personalityFiles/ExamplePersonality.json"));
        System.out.println(stateMachine);
    }

}