package roboy.dialog.states.expoStates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.context.Context;
import roboy.context.contextObjects.IntentValue;
import roboy.dialog.states.definitions.State;
import roboy.dialog.states.definitions.StateParameters;
import roboy.linguistics.Triple;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.Neo4jProperty;
import roboy.memory.Neo4jRelationship;
import roboy.memory.nodes.Interlocutor;
import roboy.memory.nodes.MemoryNodeModel;
import roboy.memory.nodes.Roboy;
import roboy.talk.PhraseCollection;
import roboy.util.QAJsonParser;
import roboy.util.RandomList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static roboy.memory.Neo4jProperty.*;

public class RoboyQAState extends State {
    public final static String INTENTS_HISTORY_ID = "RQA";

    private final String SELECTED_SKILLS = "skills";
    private final String SELECTED_ABILITIES = "abilities";
    private final String LEARN_ABOUT_PERSON = "newPerson";

    private final RandomList<String> connectingPhrases = PhraseCollection.CONNECTING_PHRASES;
    private final RandomList<String> roboyIntentPhrases = PhraseCollection.INFO_ROBOY_INTENT_PHRASES;
    private final RandomList<String> parserError = PhraseCollection.PARSER_ERROR;
    private final String INFO_FILE_PARAMETER_ID = "infoFile";

    private final Logger LOGGER = LogManager.getLogger();

    private QAJsonParser infoValues;
    private State nextState;

    public RoboyQAState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
        String infoListPath = params.getParameter(INFO_FILE_PARAMETER_ID);
        LOGGER.info(" -> The infoList path: " + infoListPath);
        infoValues = new QAJsonParser(infoListPath);
    }

    @Override
    public Output act() {
        Interlocutor person = Context.getInstance().ACTIVE_INTERLOCUTOR.getValue();
        return Output.say(String.format(connectingPhrases.getRandomElement(), person.getName()) +
                roboyIntentPhrases.getRandomElement());
    }

    @Override
    public Output react(Interpretation input) {
        Roboy roboy = new Roboy(getMemory());
        String answer = inferMemoryAnswer(input, roboy);
        nextState = getRandomTransition();
        if (answer.equals("")) {
            return Output.say(parserError.getRandomElement());
        }
        return Output.say(answer);
    }

    private String inferMemoryAnswer(Interpretation input, Roboy roboy) {
        String answer = "";
        if (input.semParserTriples != null) {
            List<Triple> triples = input.semParserTriples;
            for (Triple result : triples) {
                if (result.predicate != null) {
                    for (Neo4jRelationship predicate : Roboy.VALID_NEO4J_RELATIONSHIPS) {
                        if (result.predicate.contains(predicate.type)) {
                            String nodeName = extractNodeNameForPredicate(predicate, roboy);
                            if (nodeName != null) {
                                answer = String.format(infoValues.getSuccessAnswers(predicate).getRandomElement(), nodeName);
                                break;
                            }
                        }
                    }
                    if (!answer.equals("")){
                        break;
                    }
                }
            }
        }
        return answer;
    }

    private String extractNodeNameForPredicate(Neo4jRelationship predicate, Roboy roboy) {
        MemoryNodeModel node = getMemNodesByIds(roboy.getRelationships(predicate)).getRandomElement();
        if (node != null) {
            if (node.getProperties().containsKey(full_name.type) && !node.getProperties().get(full_name.type).equals("")) {
                return node.getProperties().get(full_name.type).toString();
            } else {
                return node.getProperties().get(name.type).toString();
            }
        }
        return null;
    }

    @Override
    public State getNextState() {
        return nextState;
    }

    private State getRandomTransition() {
        int dice = (int) (4 * Math.random() + 1);
        switch (dice) {
            case 1:
                String skill = chooseIntentAttribute(skills);
                if (!skill.equals("")) {
                    Context.getInstance().DIALOG_INTENTS_UPDATER.updateValue(new IntentValue(INTENTS_HISTORY_ID, skills, skill));
                    LOGGER.info("SELECTED_SKILLS transition");
                    return getTransition(SELECTED_SKILLS);
                } else {
                    LOGGER.info("LEARN_ABOUT_PERSON transition");
                    return getTransition(LEARN_ABOUT_PERSON);
                }
            case 2:
                String ability = chooseIntentAttribute(abilities);
                if (!ability.equals("")) {
                    Context.getInstance().DIALOG_INTENTS_UPDATER.updateValue(new IntentValue(INTENTS_HISTORY_ID, abilities, ability));
                    LOGGER.info("SELECTED_ABILITIES transition");
                    return getTransition(SELECTED_ABILITIES);
                } else {
                    LOGGER.info("LEARN_ABOUT_PERSON transition");
                    return getTransition(LEARN_ABOUT_PERSON);
                }
            case 3:
                LOGGER.info("Stay in the current state");
                return this;
            default:
                LOGGER.info("LEARN_ABOUT_PERSON transition");
                return getTransition(LEARN_ABOUT_PERSON);
        }
    }

    private String chooseIntentAttribute(Neo4jProperty predicate) {
        LOGGER.info("Trying to choose the intent attribute");
        Roboy roboy = new Roboy(getMemory());
        String attribute = "";
        HashMap<String, Object> properties = roboy.getProperties();
        if (roboy.getProperties() != null && !roboy.getProperties().isEmpty()) {
            if (properties.containsKey(predicate.type)) {
                RandomList<String> retrievedResult = new RandomList<>(Arrays.asList(properties.get(predicate.type).toString().split(",")));
                int count = 0;
                do {
                    attribute = retrievedResult.getRandomElement();
                    count++;
                } while (lastNIntentsContainAttribute(attribute, 2) && count < retrievedResult.size());
            }
        }
        LOGGER.info("The chosen attribute: " + attribute);
        return attribute;
    }

    private boolean lastNIntentsContainAttribute(String attribute, int n) {
        Map<Integer, IntentValue> lastIntentValues = Context.getInstance().DIALOG_INTENTS.getLastNValues(n);

        for (IntentValue value : lastIntentValues.values()) {
            if (value.getAttribute() != null) {
                if (value.getAttribute().equals(attribute)) {
                    return true;
                }
            }
        }
        return false;
    }
}
