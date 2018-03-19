package roboy.newDialog.states;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.context.Context;
import roboy.linguistics.Linguistics;
import roboy.linguistics.Triple;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.Neo4jMemoryInterface;
import roboy.memory.Neo4jRelationships;
import roboy.memory.nodes.Interlocutor;
import roboy.memory.nodes.Interlocutor.RelationshipAvailability;
import static roboy.memory.nodes.Interlocutor.RelationshipAvailability.*;
import roboy.memory.nodes.MemoryNodeModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static roboy.memory.Neo4jRelationships.*;


/**
 * This state will:
 * - ask the interlocutor for his name
 * - query memory if the person is already known
 * - create and update the interlocutor in the context
 * - take one of two transitions: knownPerson or newPerson
 *
 * IntroductionState interface:
 * 1) Fallback is not required.
 * TODO: maybe react() would like to have it?
 * 2) Outgoing transitions that have to be defined:
 *    - knownPerson:    following state if the person is already known
 *    - newPerson:      following state if the person is NOT known
 * 3) No parameters are used.
 */
public class IntroductionState extends State {
    private final String UPDATE_KNOWN_PERSON = "knownPerson";
    private final String LEARN_ABOUT_PERSON = "newPerson";
    private final Logger logger = LogManager.getLogger();

    private Neo4jRelationships[] predicates = { FROM, HAS_HOBBY, WORK_FOR, STUDY_AT };
    private State nextState;

    public IntroductionState(String stateIdentifier, StateParameters params) {
        super(stateIdentifier, params);
    }

    @Override
    public Output act() {
        // TODO: add more different introduction phrases
        return Output.say("I'm Roboy. What's your name?");
    }

    @Override
    public Output react(Interpretation input) {

        // expecting something like "I'm NAME"

        // parser response might look like:
        /*
        {"postags":["PRP$","NN","BE","NNP"],"answer":"(triples (triple rb:active_person rb:NAME_OF emily))",
        "lemma_tokens":["my","name","be","emily"],"tokens":["my","name","is","emily"],
        "parse":"(triples (triple rb:active_person rb:NAME_OF emily))","relations":{"(my name,be,emily)":1.0},
        "type":"question"}
        {"postags":["PRP$","NN","BE","NNP"],"answer":"(triples (triple rb:active_person rb:NAME_OF emily))",
        "lemma_tokens":["my","name","be","emily"],"tokens":["my","name","is","emily"],
        "parse":"(triples (triple rb:active_person rb:NAME_OF emily))","relations":{"(my name,be,emily)":1.0},
        "type":"statement"}
        */
        // 1. get name
        String name = getNameFromInput(input);

        if (name == null) {
            // input couldn't be parsed properly
            // TODO: do something intelligent if the parser fails
            nextState = this;
            logger.warn("IntroductionState couldn't get name! Staying in the same state.");
            return Output.say("Sorry, my parser is out of service.");
            // alternatively: Output.useFallback() or Output.sayNothing()
        }


        // 2. get interlocutor object from context
        // this also should query memory and do other magic
        Interlocutor person = Context.getInstance().ACTIVE_INTERLOCUTOR.getValue();
        person.addName(name);


        // 3. update interlocutor in context
        updateInterlocutorInContext(person);

        // 4. check if person is known/familiar
        if (person.FAMILIAR) {

            // 4a. person known/familiar
            String retrievedResult = "";
            ArrayList<Integer> ids = person.getRelationships(Neo4jRelationships.FRIEND_OF);
            if (ids != null && !ids.isEmpty()) {
                Neo4jMemoryInterface memory = getParameters().getMemory();
                try {
                    Gson gson = new Gson();
                    String requestedObject = memory.getById(ids.get(0));
                    MemoryNodeModel node = gson.fromJson(requestedObject, MemoryNodeModel.class);
                    retrievedResult = node.getProperties().get("name").toString();
                } catch (InterruptedException | IOException e) {
                    logger.error("Error on Memory data retrieval: " + e.getMessage());
                }
                retrievedResult = "! You are friends with " + retrievedResult;
            }

            RelationshipAvailability availability = person.checkRelationshipAvailability(predicates);
            if (availability == SOME_AVAILABLE) {
                nextState = (Math.random() < 0.3) ? getTransition(UPDATE_KNOWN_PERSON) : getTransition(LEARN_ABOUT_PERSON);
            } else if (availability == NONE_AVAILABLE) {
                nextState = getTransition(LEARN_ABOUT_PERSON);
            } else {
                nextState = getTransition(UPDATE_KNOWN_PERSON);
            }

            // TODO: get some friends or hobbies of the person to make answer more interesting
            return Output.say("Hey, I know you, " + person.getName() + retrievedResult);

        } else {
            // 4b. person is not known
            nextState = getTransition(LEARN_ABOUT_PERSON);

            // TODO: what would you say to a new person?
            return Output.say(String.format("Nice to meet you, %s!", name));
        }
    }

    @Override
    public State getNextState() {
        return nextState;
    }

    private String getNameFromInput(Interpretation input) {
        // TODO: call Emilka's parser
        if (input.getSentenceType().compareTo(Linguistics.SENTENCE_TYPE.STATEMENT) == 0) {
            String[] tokens = (String[]) input.getFeatures().get(Linguistics.TOKENS);
            if (tokens.length == 1) {
                return tokens[0].replace("[", "").replace("]","").toLowerCase();
            } else {
                if (input.getFeatures().get(Linguistics.PARSER_RESULT).toString().equals("SUCCESS")) {
                    List<Triple> result = (List<Triple>) input.getFeatures().get(Linguistics.SEM_TRIPLE);
                    return result.get(0).patiens.toLowerCase();
                } else {
                    if (input.getFeatures().get(Linguistics.OBJ_ANSWER) != null) {
                        String name = input.getFeatures().get(Linguistics.OBJ_ANSWER).toString().toLowerCase();
                        return !name.equals("") ? name : null;
                    }
                }
            }
        }
        return null;
    }

    private void updateInterlocutorInContext(Interlocutor interlocutor) {
        Context.getInstance().ACTIVE_INTERLOCUTOR_UPDATER.updateValue(interlocutor);
    }


    @Override
    protected Set<String> getRequiredTransitionNames() {
        return newSet(UPDATE_KNOWN_PERSON, LEARN_ABOUT_PERSON);
    }
}
