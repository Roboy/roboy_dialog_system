package roboy.memory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roboy.memory.nodes.MemoryNodeModel;
import roboy.ros.RosMainNode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the high-level-querying tasks to the Memory services using RosMainNode.
 */
public interface Neo4jMemoryInterface extends Memory<MemoryNodeModel>
{


    /**
     * Updating information in the memory for an EXISTING node with known ID.
     *
     * @param node Node with a set ID, and other properties to be set or updated.
     * @return true for success, false for fail
     */
    @Override
    boolean save(MemoryNodeModel node) throws InterruptedException, IOException;

    /**
     * This query retrieves a a single node by its ID.
     *
     * @param  id the ID of requested
     * @return String with node representation of the result.
     */
    String getById(int id) throws InterruptedException, IOException;

    /**
     * This is a classical database query which finds all matching nodes.
     *
     * @param  query the ID of requested
     * @return Array of  IDs (all nodes which correspond to the pattern).
     */
    ArrayList<Integer> getByQuery(MemoryNodeModel query) throws InterruptedException, IOException;

    int create(MemoryNodeModel query) throws InterruptedException, IOException;

    /**
     * IF ONLY THE ID IS SET, THE NODE IN MEMORY WILL BE DELETED ENTIRELY.
     * Otherwise, the properties present in the query will be deleted.
     *
     * @param query StrippedQuery avoids accidentally deleting other fields than intended.
     */
    boolean remove(MemoryNodeModel query) throws InterruptedException, IOException;
}
